package com.nexo.server;

import com.nexo.common.*;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor de Nexo. Acepta conexiones TCP y crea un {@link ClientHandler} por cliente.
 * Toda la lógica de negocio pasa por métodos sincronizados sobre la base de datos.
 */
public final class NexoServer {

    private final int port;
    final Database db;
    final Map<String, ClientHandler> online = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private volatile boolean running;

    public NexoServer(int port, Path dataFile) {
        this.port = port;
        this.db = Database.load(dataFile);
        seedIfEmpty();
    }

    /** Arranca el servidor en un hilo aparte. Lanza IOException si el puerto está ocupado. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log("Servidor Nexo escuchando en el puerto " + port);
        for (String ip : localAddresses()) {
            log("  Los clientes de la red pueden conectarse a: " + ip + ":" + port);
        }
        Thread t = new Thread(this::acceptLoop, "nexo-accept");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        for (ClientHandler h : online.values()) h.close();
        db.save();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                s.setTcpNoDelay(true);
                new ClientHandler(this, s).start();
            } catch (IOException e) {
                if (running) log("Error aceptando conexión: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- lógica

    synchronized Packet register(String username, String password, String displayName) {
        if (username == null || !username.matches("[A-Za-z0-9_.]{3,20}"))
            return error("El usuario debe tener de 3 a 20 letras, números, punto o guion bajo.");
        if (password == null || password.length() < 4)
            return error("La contraseña debe tener al menos 4 caracteres.");
        String key = username.toLowerCase();
        if (db.users.containsKey(key)) return error("Ese nombre de usuario ya existe.");
        if (displayName == null || displayName.isBlank()) displayName = username;
        User u = new User(key, displayName.trim(), password);
        db.users.put(key, u);
        db.save();
        log("Nuevo usuario registrado: " + key);
        return null;
    }

    synchronized User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User u = db.users.get(username.toLowerCase());
        return (u != null && u.checkPassword(password)) ? u : null;
    }

    /** Se llama cuando un cliente inicia sesión correctamente. */
    synchronized void onLogin(ClientHandler h, User u) {
        ClientHandler old = online.put(u.username, h);
        if (old != null && old != h) old.close();

        ArrayList<UserInfo> users = new ArrayList<>();
        for (User x : db.users.values()) users.add(x.toInfo(online.containsKey(x.username)));

        ArrayList<Chat> chats = new ArrayList<>(db.chatsOf(u.username));
        ArrayList<ChatMessage> msgs = new ArrayList<>();
        List<ChatMessage> newlyDelivered = new ArrayList<>();
        for (Chat c : chats) {
            for (ChatMessage m : db.messagesOf(c.id)) {
                if (!m.isSystem() && !u.username.equals(m.sender) && m.deliveredTo.add(u.username)) {
                    newlyDelivered.add(m);
                }
                msgs.add(m);
            }
        }
        h.send(new Packet(Packet.Type.LOGIN_OK)
                .put("me", u.toInfo(true))
                .put("users", users)
                .put("chats", chats)
                .put("messages", msgs));

        broadcast(new Packet(Packet.Type.USER_UPDATE).put("user", u.toInfo(true)), u.username);
        notifyStatus(newlyDelivered);
        if (!newlyDelivered.isEmpty()) db.save();
        log(u.username + " se conectó (" + online.size() + " en línea)");
    }

    synchronized void onDisconnect(ClientHandler h) {
        String user = h.username;
        if (user == null) return;
        if (online.get(user) != h) return; // ya hay otra sesión activa
        online.remove(user);
        User u = db.users.get(user);
        if (u != null) {
            u.lastSeen = System.currentTimeMillis();
            db.save();
            broadcast(new Packet(Packet.Type.USER_UPDATE).put("user", u.toInfo(false)), null);
        }
        log(user + " se desconectó (" + online.size() + " en línea)");
    }

    synchronized void createDirect(String me, String other) {
        if (other == null) return;
        other = other.toLowerCase();
        if (!db.users.containsKey(other) || other.equals(me)) return;
        Chat c = db.findDirect(me, other);
        if (c == null) {
            c = new Chat(UUID.randomUUID().toString(), false, null, List.of(me, other), me, System.currentTimeMillis());
            db.chats.put(c.id, c);
            db.save();
        }
        sendToMembers(c, new Packet(Packet.Type.CHAT_UPDATE).put("chat", c).put("openFor", me));
    }

    synchronized void createGroup(String me, String name, List<String> members) {
        if (name == null || name.isBlank()) return;
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(me);
        if (members != null) {
            for (String m : members) {
                String k = m.toLowerCase();
                if (db.users.containsKey(k)) set.add(k);
            }
        }
        Chat c = new Chat(UUID.randomUUID().toString(), true, name.trim(), new ArrayList<>(set), me, System.currentTimeMillis());
        db.chats.put(c.id, c);
        sendToMembers(c, new Packet(Packet.Type.CHAT_UPDATE).put("chat", c).put("openFor", me));
        User creator = db.users.get(me);
        addMessage(c, null, creator.displayName + " creó el grupo \"" + c.name + "\"", null, null, false);
    }

    synchronized void sendMessage(String me, String chatId, String text, String fileName, byte[] data, boolean image) {
        Chat c = db.chats.get(chatId);
        if (c == null || !c.members.contains(me)) return;
        boolean hasText = text != null && !text.isBlank();
        boolean hasFile = fileName != null && data != null;
        if (!hasText && !hasFile) return;
        if (hasFile && data.length > Protocol.MAX_FILE_BYTES) return;
        addMessage(c, me, hasText ? text.strip() : "", hasFile ? fileName : null, hasFile ? data : null, image);
    }

    private void addMessage(Chat c, String sender, String text, String fileName, byte[] data, boolean image) {
        ChatMessage m = new ChatMessage(UUID.randomUUID().toString(), c.id, sender, text,
                System.currentTimeMillis(), fileName, data, image);
        for (String member : c.members) {
            if (!member.equals(sender) && online.containsKey(member)) m.deliveredTo.add(member);
        }
        db.messagesOf(c.id).add(m);
        db.save();
        sendToMembers(c, new Packet(Packet.Type.NEW_MESSAGE).put("message", m));
    }

    synchronized void markRead(String me, String chatId) {
        Chat c = db.chats.get(chatId);
        if (c == null || !c.members.contains(me)) return;
        List<ChatMessage> changed = new ArrayList<>();
        for (ChatMessage m : db.messagesOf(chatId)) {
            if (m.isSystem() || me.equals(m.sender)) continue;
            boolean ch = m.readBy.add(me);
            ch |= m.deliveredTo.add(me);
            if (ch) changed.add(m);
        }
        if (!changed.isEmpty()) {
            db.save();
            notifyStatus(changed);
        }
    }

    synchronized void typing(String me, String chatId, boolean isTyping) {
        Chat c = db.chats.get(chatId);
        if (c == null || !c.members.contains(me)) return;
        Packet p = new Packet(Packet.Type.TYPING_EVENT).put("chatId", chatId).put("user", me).put("typing", isTyping);
        for (String m : c.members) {
            if (m.equals(me)) continue;
            ClientHandler h = online.get(m);
            if (h != null) h.send(p);
        }
    }

    synchronized void updateProfile(String me, String displayName, String about, byte[] avatar, boolean removeAvatar) {
        User u = db.users.get(me);
        if (u == null) return;
        if (displayName != null && !displayName.isBlank()) u.displayName = displayName.trim();
        if (about != null) u.about = about.trim();
        if (removeAvatar) u.avatar = null;
        else if (avatar != null && avatar.length <= 512 * 1024) u.avatar = avatar;
        db.save();
        broadcast(new Packet(Packet.Type.USER_UPDATE).put("user", u.toInfo(true)), null);
    }

    /** Avisa a los miembros de cada chat de los cambios de estado de sus mensajes. */
    private void notifyStatus(List<ChatMessage> changed) {
        Map<String, ArrayList<StatusUpdate>> byChat = new HashMap<>();
        for (ChatMessage m : changed) byChat.computeIfAbsent(m.chatId, k -> new ArrayList<>()).add(new StatusUpdate(m));
        for (Map.Entry<String, ArrayList<StatusUpdate>> e : byChat.entrySet()) {
            Chat c = db.chats.get(e.getKey());
            if (c != null) sendToMembers(c, new Packet(Packet.Type.STATUS_UPDATE).put("updates", e.getValue()));
        }
    }

    private void sendToMembers(Chat c, Packet p) {
        for (String m : c.members) {
            ClientHandler h = online.get(m);
            if (h != null) h.send(p);
        }
    }

    private void broadcast(Packet p, String except) {
        for (Map.Entry<String, ClientHandler> e : online.entrySet()) {
            if (!e.getKey().equals(except)) e.getValue().send(p);
        }
    }

    static Packet error(String msg) {
        return new Packet(Packet.Type.ERROR).put("message", msg);
    }

    /** Crea usuarios y chats de ejemplo la primera vez, para poder hacer la demo enseguida. */
    private void seedIfEmpty() {
        if (!db.users.isEmpty()) return;
        register("ana", "1234", "Ana Martínez");
        register("luis", "1234", "Luis Gómez");
        register("sofia", "1234", "Sofía Ramírez");
        long now = System.currentTimeMillis();
        Chat direct = new Chat(UUID.randomUUID().toString(), false, null, List.of("ana", "luis"), "ana", now);
        db.chats.put(direct.id, direct);
        ChatMessage m1 = new ChatMessage(UUID.randomUUID().toString(), direct.id, "ana",
                "¡Hola Luis! ¿Listo para la presentación?", now - 60_000, null, null, false);
        m1.deliveredTo.add("luis");
        m1.readBy.add("luis");
        ChatMessage m2 = new ChatMessage(UUID.randomUUID().toString(), direct.id, "luis",
                "¡Claro! Nexo ya funciona en red local 😄", now - 30_000, null, null, false);
        db.messagesOf(direct.id).add(m1);
        db.messagesOf(direct.id).add(m2);
        Chat group = new Chat(UUID.randomUUID().toString(), true, "Proyecto Nexo", List.of("ana", "luis", "sofia"), "ana", now);
        db.chats.put(group.id, group);
        db.messagesOf(group.id).add(new ChatMessage(UUID.randomUUID().toString(), group.id, null,
                "Ana Martínez creó el grupo \"Proyecto Nexo\"", now - 120_000, null, null, false));
        db.save();
        log("Se crearon usuarios de ejemplo: ana, luis, sofia (contraseña 1234)");
    }

    static List<String> localAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) out.add(a.getHostAddress());
                }
            }
        } catch (SocketException ignored) {
        }
        return out;
    }

    private static final SimpleDateFormat LOG_FMT = new SimpleDateFormat("HH:mm:ss");

    /** Si no es null, también recibe cada línea del registro (lo usa la ventana del servidor). */
    public static volatile java.util.function.Consumer<String> logListener;

    static synchronized void log(String msg) {
        String line = "[" + LOG_FMT.format(new Date()) + "] " + msg;
        System.out.println(line);
        if (logListener != null) logListener.accept(line);
    }

    /** Punto de entrada para correr solo el servidor. */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        NexoServer server = new NexoServer(port, Path.of("nexo-data", "nexo.db"));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        log("Presiona Ctrl+C para detener el servidor.");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {
        }
    }
}
