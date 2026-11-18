package com.nexo.server;

import com.nexo.client.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/** Ventana sencilla que muestra el registro del servidor. */
public final class ServerWindow extends JFrame {

    public ServerWindow(int port) {
        super("Nexo - Servidor (puerto " + port + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setMargin(new Insets(10, 10, 10, 10));
        JLabel info = new JLabel("  Cierra esta ventana para apagar el servidor.");
        info.setFont(Theme.font(Font.PLAIN, 13));
        info.setPreferredSize(new Dimension(10, 32));
        add(info, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        setSize(720, 440);
        setLocationRelativeTo(null);

        NexoServer.logListener = line -> SwingUtilities.invokeLater(() -> {
            area.append(line + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
        try {
            NexoServer s = new NexoServer(port, Path.of("nexo-data", "nexo.db"));
            s.start();
            Runtime.getRuntime().addShutdownHook(new Thread(s::stop));
        } catch (Exception e) {
            NexoServer.log("No se pudo iniciar el servidor: " + e.getMessage()
                    + "\n¿Ya hay otro servidor de Nexo abierto en el puerto " + port + "?");
        }
    }
}
