package com.nexo.server;

import com.nexo.common.Packet;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/** Atiende a un cliente conectado: lee sus paquetes y le envía los del servidor. */
final class ClientHandler extends Thread {

    private final NexoServer server;
    private final Socket socket;
    private ObjectOutputStream out;
    volatile String username;

    ClientHandler(NexoServer server, Socket socket) {
        super("nexo-client-" + socket.getRemoteSocketAddress());
        setDaemon(true);
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            while (!socket.isClosed()) {
                Object o = in.readObject();
                if (o instanceof Packet p) handle(p);
            }
        } catch (EOFException | java.net.SocketException ignored) {
            // el cliente cerró la conexión
        } catch (Exception e) {
            NexoServer.log("Error con cliente " + username + ": " + e);
        } finally {
            close();
            server.onDisconnect(this);
        }
    }

    private void handle(Packet p) {
        if (username == null) {
            handleAuth(p);
            return;
        }
        switch (p.getType()) {
            case CREATE_DIRECT -> server.createDirect(username, p.str("username"));
            case CREATE_GROUP -> {
                ArrayList<String> members = p.get("members");
                server.createGroup(username, p.str("name"), members);
            }
            case SEND_MESSAGE -> server.sendMessage(username, p.str("chatId"), p.str("text"),
                    p.str("fileName"), p.get("fileData"), p.bool("image"));
            case MARK_READ -> server.markRead(username, p.str("chatId"));
            case TYPING -> server.typing(username, p.str("chatId"), p.bool("typing"));
            case UPDATE_PROFILE -> server.updateProfile(username, p.str("displayName"), p.str("about"));
            case LOGOUT -> close();
            default -> { }
        }
    }

    private void handleAuth(Packet p) {
        String user = p.str("username");
        String pass = p.str("password");
        if (p.getType() == Packet.Type.REGISTER) {
            Packet err = server.register(user, pass, p.str("displayName"));
            if (err != null) {
                send(err);
                return;
            }
        } else if (p.getType() != Packet.Type.LOGIN) {
            send(NexoServer.error("Primero inicia sesión."));
            return;
        }
        User u = server.authenticate(user, pass);
        if (u == null) {
            send(NexoServer.error("Usuario o contraseña incorrectos."));
            return;
        }
        username = u.username;
        server.onLogin(this, u);
    }

    void send(Packet p) {
        ObjectOutputStream o = out;
        if (o == null) return;
        synchronized (this) {
            try {
                o.writeObject(p);
                o.reset(); // evita que se reenvíen copias viejas de objetos ya enviados
                o.flush();
            } catch (IOException e) {
                close();
            }
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
