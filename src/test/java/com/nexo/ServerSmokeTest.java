package com.nexo;

import com.nexo.common.Chat;
import com.nexo.common.ChatMessage;
import com.nexo.common.Packet;
import com.nexo.common.StatusUpdate;
import com.nexo.server.NexoServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Prueba de integración del protocolo TCP del servidor, sin librerías externas. */
public final class ServerSmokeTest {

    private static final Duration WAIT = Duration.ofSeconds(8);

    private ServerSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path dataDirectory = Files.createTempDirectory("nexo-smoke-");
        Path dataFile = dataDirectory.resolve("nexo.db");
        String anaName = "smokeana";
        String luisName = "smokeluis";
        String sofiaName = "smokesofia";
        String[] persistedIds = new String[3];

        int initialPort = freePort();
        NexoServer server = new NexoServer(initialPort, dataFile);
        try {
            server.start();
            try (TestClient ana = new TestClient(initialPort);
                 TestClient luis = new TestClient(initialPort);
                 TestClient sofia = new TestClient(initialPort)) {
                ana.send(new Packet(Packet.Type.LOGIN)
                        .put("username", "ana")
                        .put("password", "incorrecta"));
                Packet badLogin = ana.await(Packet.Type.ERROR);
                check(badLogin.str("message") != null, "LOGIN incorrecto debe explicar el error");

                ana.register(anaName, "clave123", "Ana de prueba");
                luis.register(luisName, "clave123", "Luis de prueba");
                sofia.register(sofiaName, "clave123", "Sofía de prueba");

                ana.send(new Packet(Packet.Type.CREATE_DIRECT).put("username", luisName));
                Chat direct = (Chat) ana.await(Packet.Type.CHAT_UPDATE).get("chat");
                Chat directForLuis = (Chat) luis.await(Packet.Type.CHAT_UPDATE).get("chat");
                check(direct != null && !direct.group, "CREATE_DIRECT debe producir un chat individual");
                check(direct.id.equals(directForLuis.id), "Ambos clientes deben recibir el mismo chat directo");
                persistedIds[0] = direct.id;

                ana.send(new Packet(Packet.Type.SEND_MESSAGE)
                        .put("chatId", persistedIds[0])
                        .put("text", "Mensaje de prueba con ñ y tilde: canción"));
                ChatMessage sentByAna = (ChatMessage) ana.await(Packet.Type.NEW_MESSAGE).get("message");
                ChatMessage receivedByLuis = (ChatMessage) luis.await(Packet.Type.NEW_MESSAGE).get("message");
                check(sentByAna != null && sentByAna.id.equals(receivedByLuis.id),
                        "NEW_MESSAGE debe llegar al emisor y al destinatario");
                check(receivedByLuis.text.contains("canción"), "El texto Unicode debe conservarse");
                check(receivedByLuis.deliveredTo.contains(luisName), "El mensaje debe marcarse entregado a Luis conectado");
                persistedIds[1] = sentByAna.id;

                luis.send(new Packet(Packet.Type.MARK_READ).put("chatId", persistedIds[0]));
                StatusUpdate readByAna = findStatusFor(ana.await(Packet.Type.STATUS_UPDATE), persistedIds[1]);
                StatusUpdate readByLuis = findStatusFor(luis.await(Packet.Type.STATUS_UPDATE), persistedIds[1]);
                check(readByAna.readBy.contains(luisName), "STATUS_UPDATE debe incluir a Luis en readBy");
                check(readByLuis.readBy.contains(luisName), "El destinatario también debe recibir readBy actualizado");

                ana.send(new Packet(Packet.Type.CREATE_GROUP)
                        .put("name", "Grupo smoke")
                        .put("members", new ArrayList<>(List.of(luisName, sofiaName))));
                Chat groupForAna = (Chat) ana.await(Packet.Type.CHAT_UPDATE).get("chat");
                Chat groupForLuis = (Chat) luis.await(Packet.Type.CHAT_UPDATE).get("chat");
                Chat groupForSofia = (Chat) sofia.await(Packet.Type.CHAT_UPDATE).get("chat");
                check(groupForAna != null && groupForAna.group, "CREATE_GROUP debe crear un grupo");
                check(groupForAna.members.size() == 3, "El grupo debe incluir a tres miembros");
                check(groupForAna.id.equals(groupForLuis.id) && groupForAna.id.equals(groupForSofia.id),
                        "Los tres clientes deben recibir el mismo grupo");
                persistedIds[2] = groupForAna.id;

                luis.send(new Packet(Packet.Type.TYPING)
                        .put("chatId", persistedIds[0])
                        .put("typing", true));
                Packet typing = ana.await(Packet.Type.TYPING_EVENT);
                check(persistedIds[0].equals(typing.str("chatId")), "TYPING_EVENT debe incluir el chat correcto");
                check(luisName.equals(typing.str("user")) && typing.bool("typing"),
                        "TYPING_EVENT debe identificar al usuario y el estado");
            }
        } finally {
            server.stop();
        }

        int reloadPort = freePort();
        NexoServer reloaded = new NexoServer(reloadPort, dataFile);
        try {
            reloaded.start();
            try (TestClient anaAfterRestart = new TestClient(reloadPort)) {
                anaAfterRestart.login(anaName, "clave123");
                Packet login = anaAfterRestart.await(Packet.Type.LOGIN_OK);
                ArrayList<Chat> chats = login.get("chats");
                ArrayList<ChatMessage> messages = login.get("messages");
                check(chats.stream().anyMatch(chat -> chat.id.equals(persistedIds[0])),
                        "El chat directo debe persistir al reiniciar el servidor");
                check(chats.stream().anyMatch(chat -> chat.id.equals(persistedIds[2])
                                && chat.members.size() == 3),
                        "El grupo de tres debe persistir al reiniciar el servidor");
                check(messages.stream().anyMatch(message -> message.id.equals(persistedIds[1])
                                && message.text.contains("canción")),
                        "El mensaje enviado debe persistir al reiniciar el servidor");
            }
        } finally {
            reloaded.stop();
        }

        System.out.println("OK");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static StatusUpdate findStatusFor(Packet packet, String messageId) {
        ArrayList<StatusUpdate> updates = packet.get("updates");
        if (updates != null) {
            for (StatusUpdate update : updates) {
                if (messageId.equals(update.messageId)) return update;
            }
        }
        throw new AssertionError("STATUS_UPDATE no contiene el mensaje " + messageId);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final ObjectOutputStream output;
        private final BlockingQueue<Packet> incoming = new LinkedBlockingQueue<>();

        TestClient(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            output.flush();

            Thread reader = new Thread(() -> readPackets(input), "nexo-smoke-reader");
            reader.setDaemon(true);
            reader.start();
        }

        void register(String username, String password, String displayName) throws IOException, InterruptedException {
            send(new Packet(Packet.Type.REGISTER)
                    .put("username", username)
                    .put("password", password)
                    .put("displayName", displayName));
            await(Packet.Type.LOGIN_OK);
        }

        void login(String username, String password) throws IOException {
            send(new Packet(Packet.Type.LOGIN)
                    .put("username", username)
                    .put("password", password));
        }

        synchronized void send(Packet packet) throws IOException {
            output.writeObject(packet);
            output.reset();
            output.flush();
        }

        Packet await(Packet.Type expected) throws InterruptedException {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (true) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new AssertionError("Tiempo agotado esperando " + expected);
                Packet packet = incoming.poll(remaining, TimeUnit.NANOSECONDS);
                if (packet == null) throw new AssertionError("Tiempo agotado esperando " + expected);
                if (packet.getType() == expected) return packet;
                if (packet.getType() == Packet.Type.ERROR) {
                    throw new AssertionError("El servidor respondió ERROR al esperar " + expected
                            + ": " + packet.str("message"));
                }
            }
        }

        private void readPackets(ObjectInputStream input) {
            try {
                while (!socket.isClosed()) {
                    Object object = input.readObject();
                    if (object instanceof Packet packet) incoming.offer(packet);
                }
            } catch (EOFException | SocketException ignored) {
                // El cierre del cliente o servidor termina el lector.
            } catch (Exception error) {
                incoming.offer(new Packet(Packet.Type.ERROR)
                        .put("message", "Fallo leyendo respuesta: " + error.getMessage()));
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
