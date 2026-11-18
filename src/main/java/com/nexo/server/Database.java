package com.nexo.server;

import com.nexo.common.Chat;
import com.nexo.common.ChatMessage;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Almacenamiento del servidor. Se guarda en disco (carpeta nexo-data)
 * con serialización de Java, así no hace falta instalar ninguna base de datos.
 */
final class Database implements Serializable {
    private static final long serialVersionUID = 1L;

    final LinkedHashMap<String, User> users = new LinkedHashMap<>();
    final LinkedHashMap<String, Chat> chats = new LinkedHashMap<>();
    final HashMap<String, ArrayList<ChatMessage>> messages = new HashMap<>();

    private transient Path file;

    static Database load(Path file) {
        Database db = null;
        if (Files.exists(file)) {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                db = (Database) in.readObject();
                NexoServer.log("Datos cargados desde " + file.toAbsolutePath());
            } catch (Exception e) {
                NexoServer.log("No se pudieron leer los datos (" + e.getMessage() + "). Se empieza vacío.");
            }
        }
        if (db == null) db = new Database();
        db.file = file;
        return db;
    }

    synchronized void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeObject(this);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            NexoServer.log("Error al guardar datos: " + e.getMessage());
        }
    }

    List<ChatMessage> messagesOf(String chatId) {
        return messages.computeIfAbsent(chatId, k -> new ArrayList<>());
    }

    List<Chat> chatsOf(String username) {
        List<Chat> out = new ArrayList<>();
        for (Chat c : chats.values()) {
            if (c.members.contains(username)) out.add(c);
        }
        return out;
    }

    Chat findDirect(String a, String b) {
        for (Chat c : chats.values()) {
            if (!c.group && c.members.contains(a) && c.members.contains(b)) return c;
        }
        return null;
    }
}
