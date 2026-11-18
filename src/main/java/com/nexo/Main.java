package com.nexo;

import com.nexo.client.ui.LoginFrame;
import com.nexo.client.ui.Theme;
import com.nexo.common.Protocol;
import com.nexo.server.NexoServer;
import com.nexo.server.ServerWindow;

import javax.swing.*;
import java.awt.*;
import java.net.BindException;
import java.nio.file.Path;

/**
 * Punto de entrada de Nexo.
 * <pre>
 *   java -jar Nexo.jar                 -> abre el lanzador
 *   java -jar Nexo.jar demo            -> servidor + 2 clientes (ana y luis)
 *   java -jar Nexo.jar server [puerto] -> solo servidor (consola)
 *   java -jar Nexo.jar client [host] [puerto]
 * </pre>
 */
public final class Main {

    private static NexoServer embedded;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase() : "";
        if (mode.equals("server")) {
            NexoServer.main(args.length > 1 ? new String[]{args[1]} : new String[0]);
            return;
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeAndWait(Theme::installDefaults);
        switch (mode) {
            case "demo" -> SwingUtilities.invokeLater(Main::demo);
            case "client" -> {
                String host = args.length > 1 ? args[1] : "localhost";
                int port = args.length > 2 ? Integer.parseInt(args[2]) : Protocol.DEFAULT_PORT;
                SwingUtilities.invokeLater(() -> new LoginFrame(host, port, null, null).setVisible(true));
            }
            default -> SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
        }
    }

    /** Arranca un servidor dentro de este programa. Si el puerto ya está ocupado, usa ese servidor. */
    static boolean ensureServer(Component parent, int port) {
        if (embedded != null) return true;
        try {
            NexoServer s = new NexoServer(port, Path.of("nexo-data", "nexo.db"));
            s.start();
            embedded = s;
            Runtime.getRuntime().addShutdownHook(new Thread(s::stop));
            return true;
        } catch (BindException e) {
            // ya hay un servidor (u otro programa) en ese puerto: intentamos usarlo
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "No se pudo iniciar el servidor: " + e.getMessage());
            return false;
        }
    }

    /** Modo demo: servidor + dos ventanas lado a lado con sesión iniciada. */
    static void demo() {
        if (!ensureServer(null, Protocol.DEFAULT_PORT)) return;
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int w = screen.width / 2;
        String[][] users = {{"ana", "1234"}, {"luis", "1234"}};
        for (int i = 0; i < users.length; i++) {
            LoginFrame f = new LoginFrame("localhost", Protocol.DEFAULT_PORT, users[i][0], users[i][1]);
            Rectangle r = new Rectangle(screen.x + i * w, screen.y, w, screen.height);
            f.setLocation(r.x + (w - f.getWidth()) / 2, r.y + 20);
            f.setMainBounds(r);
            f.setVisible(true);
            f.autoLogin();
        }
    }

    /** Ventana inicial con las opciones de arranque. */
    static final class Launcher extends JFrame {
        Launcher() {
            super("Nexo");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));

            JLabel logo = new JLabel(com.nexo.client.ui.Icons.logo(80));
            JLabel title = new JLabel("Nexo");
            title.setFont(Theme.font(Font.BOLD, 30));
            JLabel sub = new JLabel("¿Cómo quieres iniciar?");
            sub.setFont(Theme.font(Font.PLAIN, 15));
            sub.setForeground(Theme.MUTED);
            for (JComponent c : new JComponent[]{logo, title, sub}) {
                c.setAlignmentX(CENTER_ALIGNMENT);
                root.add(c);
            }
            root.add(Box.createVerticalStrut(18));

            addOption(root, "Demo en este PC", "Servidor + 2 ventanas (Ana y Luis) listas para chatear", true, () -> {
                dispose();
                demo();
            });
            addOption(root, "Ser el servidor y entrar", "Este PC hace de servidor; otros se conectan por la red", false, () -> {
                if (!ensureServer(this, Protocol.DEFAULT_PORT)) return;
                dispose();
                new LoginFrame("localhost", Protocol.DEFAULT_PORT, null, null).setVisible(true);
            });
            addOption(root, "Conectarme a un servidor", "Escribe la IP del PC que hace de servidor", false, () -> {
                dispose();
                new LoginFrame("", Protocol.DEFAULT_PORT, null, null).setVisible(true);
            });
            addOption(root, "Solo servidor", "Muestra el registro del servidor y las IPs para conectarse", false, () -> {
                dispose();
                new ServerWindow(Protocol.DEFAULT_PORT).setVisible(true);
            });

            setContentPane(root);
            setIconImage(iconOf());
            pack();
            setResizable(false);
            setLocationRelativeTo(null);
        }

        private static Image iconOf() {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            com.nexo.client.ui.Icons.logo(64).paintIcon(null, g, 0, 0);
            g.dispose();
            return img;
        }

        private void addOption(JPanel root, String title, String desc, boolean primary, Runnable action) {
            JButton b = Theme.button("<html><div style='text-align:center'><b>" + title + "</b><br>"
                    + "<span style='font-size:10px'>" + desc + "</span></div></html>", primary);
            b.setAlignmentX(CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(420, 64));
            b.setPreferredSize(new Dimension(420, 64));
            b.addActionListener(e -> action.run());
            root.add(b);
            root.add(Box.createVerticalStrut(10));
        }
    }
}
