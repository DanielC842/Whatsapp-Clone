package com.nexo.server;

import com.nexo.common.UserInfo;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Usuario tal como lo guarda el servidor (con hash de contraseña). */
final class User implements Serializable {
    private static final long serialVersionUID = 1L;

    final String username;
    String displayName;
    String about = "¡Hola! Estoy usando Nexo.";
    final String salt;
    final String passwordHash;
    long lastSeen;

    User(String username, String displayName, String password) {
        this.username = username;
        this.displayName = displayName;
        byte[] s = new byte[16];
        new SecureRandom().nextBytes(s);
        this.salt = Base64.getEncoder().encodeToString(s);
        this.passwordHash = hash(password, salt);
        this.lastSeen = System.currentTimeMillis();
    }

    boolean checkPassword(String password) {
        return passwordHash.equals(hash(password, salt));
    }

    UserInfo toInfo(boolean online) {
        return new UserInfo(username, displayName, about, online, lastSeen);
    }

    private static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] out = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
