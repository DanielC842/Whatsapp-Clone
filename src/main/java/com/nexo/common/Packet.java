package com.nexo.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Paquete que viaja entre cliente y servidor.
 * Lleva un tipo y un mapa de datos serializables.
 */
public final class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        // Cliente -> Servidor
        REGISTER, LOGIN, LOGOUT, CREATE_DIRECT, CREATE_GROUP, SEND_MESSAGE, MARK_READ, TYPING, UPDATE_PROFILE,
        // Servidor -> Cliente
        LOGIN_OK, ERROR, USER_UPDATE, CHAT_UPDATE, NEW_MESSAGE, STATUS_UPDATE, TYPING_EVENT
    }

    private final Type type;
    private final HashMap<String, Serializable> data = new HashMap<>();

    public Packet(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Packet put(String key, Serializable value) {
        data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public String str(String key) {
        Object v = data.get(key);
        return v == null ? null : v.toString();
    }

    public boolean bool(String key) {
        Object v = data.get(key);
        return v instanceof Boolean b && b;
    }

    @Override
    public String toString() {
        return "Packet[" + type + " " + data.keySet() + "]";
    }
}
