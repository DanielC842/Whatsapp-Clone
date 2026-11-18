package com.nexo.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

/** Un mensaje dentro de un chat, con texto y/o archivo adjunto. */
public final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { SENT, DELIVERED, READ }

    public final String id;
    public final String chatId;
    public final String sender;        // null = mensaje del sistema
    public final String text;
    public final long timestamp;
    public final String fileName;      // null si no hay adjunto
    public final byte[] fileData;
    public final boolean image;
    public final HashSet<String> deliveredTo = new HashSet<>();
    public final HashSet<String> readBy = new HashSet<>();

    public ChatMessage(String id, String chatId, String sender, String text, long timestamp,
                       String fileName, byte[] fileData, boolean image) {
        this.id = id;
        this.chatId = chatId;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
        this.fileName = fileName;
        this.fileData = fileData;
        this.image = image;
    }

    public boolean isSystem() {
        return sender == null;
    }

    public boolean hasFile() {
        return fileName != null && fileData != null;
    }

    /** Estado visto por el remitente: enviado, entregado a todos o leído por todos. */
    public Status statusFor(List<String> members) {
        boolean allDelivered = true;
        boolean allRead = true;
        for (String m : members) {
            if (m.equals(sender)) continue;
            if (!readBy.contains(m)) allRead = false;
            if (!deliveredTo.contains(m) && !readBy.contains(m)) allDelivered = false;
        }
        if (allRead) return Status.READ;
        if (allDelivered) return Status.DELIVERED;
        return Status.SENT;
    }
}
