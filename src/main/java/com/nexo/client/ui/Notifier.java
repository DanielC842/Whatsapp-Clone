package com.nexo.client.ui;

import java.awt.*;

/**
 * Notificaciones de escritorio (icono en la bandeja del sistema).
 * Si el sistema no tiene bandeja, simplemente no hace nada.
 */
final class Notifier {

    private static TrayIcon icon;
    private static boolean tried;

    private Notifier() {
    }

    private static synchronized TrayIcon icon() {
        if (tried) return icon;
        tried = true;
        try {
            if (!SystemTray.isSupported()) return null;
            TrayIcon t = new TrayIcon(MainFrame.iconImage(), "Nexo");
            t.setImageAutoSize(true);
            SystemTray.getSystemTray().add(t);
            icon = t;
        } catch (Exception | Error e) {
            icon = null;
        }
        return icon;
    }

    static void show(String title, String text) {
        TrayIcon t = icon();
        if (t == null) return;
        String body = text.length() > 120 ? text.substring(0, 117) + "..." : text;
        t.displayMessage(title, body, TrayIcon.MessageType.NONE);
    }

    static void dispose() {
        if (icon != null) {
            try {
                SystemTray.getSystemTray().remove(icon);
            } catch (Exception ignored) {
            }
        }
    }
}
