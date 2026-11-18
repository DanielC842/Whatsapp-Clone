package com.nexo.client;

import com.nexo.common.Packet;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Conexión TCP del cliente con el servidor.
 * Los paquetes recibidos se entregan en el hilo de Swing (EDT).
 */
public final class NexoConnection {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile Consumer<Packet> handler = p -> { };
    private volatile Runnable onClose = () -> { };
    private volatile boolean closed;

    private NexoConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.out.flush();
        this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    public static NexoConnection connect(String host, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 4000);
        s.setTcpNoDelay(true);
        NexoConnection c = new NexoConnection(s);
        Thread t = new Thread(c::readLoop, "nexo-reader");
        t.setDaemon(true);
        t.start();
        return c;
    }

    public void setHandler(Consumer<Packet> handler) {
        this.handler = handler;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void readLoop() {
        try {
            while (!closed) {
                Object o = in.readObject();
                if (o instanceof Packet p) SwingUtilities.invokeLater(() -> handler.accept(p));
            }
        } catch (Exception e) {
            // conexión cerrada
        }
        boolean wasOpen = !closed;
        close();
        if (wasOpen) SwingUtilities.invokeLater(() -> onClose.run());
    }

    public synchronized void send(Packet p) {
        if (closed) return;
        try {
            out.writeObject(p);
            out.reset();
            out.flush();
        } catch (IOException e) {
            close();
        }
    }

    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
