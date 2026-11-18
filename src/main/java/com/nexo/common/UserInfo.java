package com.nexo.common;

import java.io.Serializable;

/** Datos públicos de un usuario (nunca incluye la contraseña). */
public final class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String username;
    public final String displayName;
    public final String about;
    public final boolean online;
    public final long lastSeen;
    /** Foto de perfil en PNG (puede ser null). */
    public final byte[] avatar;

    public UserInfo(String username, String displayName, String about, boolean online, long lastSeen, byte[] avatar) {
        this.username = username;
        this.displayName = displayName;
        this.about = about;
        this.online = online;
        this.lastSeen = lastSeen;
        this.avatar = avatar;
    }
}
