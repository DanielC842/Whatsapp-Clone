package com.nexo.client;

import com.nexo.common.*;

import java.util.*;

/** Estado local del cliente: usuarios, chats, mensajes y contadores. */
public final class ChatStore {

    public UserInfo me;
    public final Map<String, UserInfo> users = new HashMap<>();
    public final Map<String, Chat> chats = new LinkedHashMap<>();
    public final Map<String, List<ChatMessage>> messages = new HashMap<>();
    public final Map<String, Integer> unread = new HashMap<>();
    /** chatId -> usuarios escribiendo (con la hora en que avisaron). */
    public final Map<String, Map<String, Long>> typing = new HashMap<>();

    public void load(Packet loginOk) {
        me = loginOk.get("me");
        ArrayList<UserInfo> us = loginOk.get("users");
        for (UserInfo u : us) users.put(u.username, u);
        ArrayList<Chat> cs = loginOk.get("chats");
        for (Chat c : cs) chats.put(c.id, c);
        ArrayList<ChatMessage> ms = loginOk.get("messages");
        for (ChatMessage m : ms) {
            messagesOf(m.chatId).add(m);
            if (!m.isSystem() && !m.sender.equals(me.username) && !m.readBy.contains(me.username)) {
                unread.merge(m.chatId, 1, Integer::sum);
            }
        }
    }

    public List<ChatMessage> messagesOf(String chatId) {
        return messages.computeIfAbsent(chatId, k -> new ArrayList<>());
    }

    public ChatMessage lastMessage(String chatId) {
        List<ChatMessage> l = messages.get(chatId);
        return (l == null || l.isEmpty()) ? null : l.get(l.size() - 1);
    }

    public long lastActivity(String chatId) {
        ChatMessage m = lastMessage(chatId);
        return m != null ? m.timestamp : chats.get(chatId).createdAt;
    }

    public String displayName(String username) {
        if (username == null) return "";
        UserInfo u = users.get(username);
        return u != null ? u.displayName : username;
    }

    /** Nombre que se muestra para un chat (nombre del grupo o del otro usuario). */
    public String chatTitle(Chat c) {
        return c.group ? c.name : displayName(c.otherMember(me.username));
    }

    /** Chats ordenados por actividad reciente. */
    public List<Chat> sortedChats() {
        List<Chat> l = new ArrayList<>(chats.values());
        l.sort((a, b) -> Long.compare(lastActivity(b.id), lastActivity(a.id)));
        return l;
    }

    public void applyStatus(StatusUpdate s) {
        for (ChatMessage m : messagesOf(s.chatId)) {
            if (m.id.equals(s.messageId)) {
                m.deliveredTo.clear();
                m.deliveredTo.addAll(s.deliveredTo);
                m.readBy.clear();
                m.readBy.addAll(s.readBy);
                return;
            }
        }
    }

    /** Usuarios que están escribiendo en un chat (se olvidan tras 6 segundos sin aviso). */
    public List<String> typingIn(String chatId) {
        Map<String, Long> t = typing.get(chatId);
        if (t == null) return List.of();
        long now = System.currentTimeMillis();
        t.values().removeIf(time -> now - time > 6000);
        return new ArrayList<>(t.keySet());
    }

    public int totalUnread() {
        int n = 0;
        for (int v : unread.values()) n += v;
        return n;
    }
}
