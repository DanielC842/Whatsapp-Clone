package com.nexo.common;

import java.io.Serializable;
import java.util.HashSet;

/** Cambio de estado (entregado/leído) de un mensaje. */
public final class StatusUpdate implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String chatId;
    public final String messageId;
    public final HashSet<String> deliveredTo;
    public final HashSet<String> readBy;

    public StatusUpdate(ChatMessage m) {
        this.chatId = m.chatId;
        this.messageId = m.id;
        this.deliveredTo = new HashSet<>(m.deliveredTo);
        this.readBy = new HashSet<>(m.readBy);
    }
}
