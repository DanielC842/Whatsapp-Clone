package com.nexo.client.ui;

import com.nexo.client.NexoConnection;
import com.nexo.common.Packet;
import com.nexo.common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Pantalla de inicio de sesión y registro. */
public final class LoginFrame extends JFrame {

    private final JTextField host = Theme.textField("Servidor (ej. localhost o 192.168.1.10)");
    private final JTextField port = Theme.textField("Puerto");
    private final JTextField user = Theme.textField("Usuario");
    private final JPasswordField pass = Theme.passwordField("Contraseña");
    private final JTextField display = Theme.textField("Tu nombre (ej. Ana Martínez)");
    private final JButton primary = Theme.button("Entrar", true);
    private final JLabel toggle = new JLabel();
    private final JLabel status = new JLabel(" ");
    private final JPanel displayRow;
    private boolean registerMode;
    private NexoConnection conn;
    private Rectangle mainBounds;

    public LoginFrame(String defaultHost, int defaultPort, String prefillUser, String prefillPass) {
        super(Protocol.APP_NAME + " - Iniciar sesión");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(MainFrame.iconImage());
        host.setText(defaultHost == null ? "localhost" : defaultHost);
        port.setText(String.valueOf(defaultPort));
        if (prefillUser != null) user.setText(prefillUser);
        if (prefillPass != null) pass.setText(prefillPass);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.CHAT_BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.DIVIDER, 1, true), new EmptyBorder(28, 32, 24, 32)));

        JLabel logo = new JLabel(Icons.logo(76));
        JLabel title = new JLabel("Nexo");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(Theme.TEXT);
        JLabel sub = new JLabel("Mensajería rápida en tu red");
        sub.setFont(Theme.font(Font.PLAIN, 14));
        sub.setForeground(Theme.MUTED);

        JPanel serverRow = new JPanel(new BorderLayout(8, 0));
        serverRow.setOpaque(false);
        port.setColumns(5);
        serverRow.add(host, BorderLayout.CENTER);
        serverRow.add(port, BorderLayout.EAST);
        displayRow = row(display);

        status.setFont(Theme.font(Font.PLAIN, 13));
        status.setForeground(new Color(0xDC2626));
        toggle.setFont(Theme.font(Font.PLAIN, 13));
        toggle.setForeground(Theme.PRIMARY);
        Theme.onClick(toggle, () -> setRegisterMode(!registerMode));

        primary.setAlignmentX(CENTER_ALIGNMENT);
        primary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        primary.addActionListener(e -> submit());
        getRootPane().setDefaultButton(primary);

        for (JComponent c : new JComponent[]{logo, title, sub}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            card.add(c);
        }
        card.add(Box.createVerticalStrut(18));
        card.add(label("Servidor"));
        card.add(serverRow);
        card.add(Box.createVerticalStrut(10));
        card.add(label("Usuario"));
        card.add(row(user));
        card.add(Box.createVerticalStrut(10));
        card.add(label("Contraseña"));
        card.add(row(pass));
        card.add(Box.createVerticalStrut(10));
        card.add(displayRow);
        card.add(Box.createVerticalStrut(8));
        status.setAlignmentX(CENTER_ALIGNMENT);
        card.add(status);
        card.add(Box.createVerticalStrut(8));
        card.add(primary);
        card.add(Box.createVerticalStrut(12));
        toggle.setAlignmentX(CENTER_ALIGNMENT);
        card.add(toggle);
        card.setPreferredSize(new Dimension(400, card.getPreferredSize().height + 70));

        root.add(card);
        setContentPane(root);
        setRegisterMode(false);
        setSize(520, 700);
        setLocationRelativeTo(null);
    }

    private JPanel row(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c);
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return p;
    }

    private JPanel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 13));
        l.setForeground(Theme.MUTED);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        p.setOpaque(false);
        p.add(l);
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return p;
    }

    private void setRegisterMode(boolean reg) {
        registerMode = reg;
        displayRow.setVisible(reg);
        primary.setText(reg ? "Crear cuenta" : "Entrar");
        toggle.setText(reg ? "¿Ya tienes cuenta? Inicia sesión" : "¿No tienes cuenta? Regístrate");
        status.setText(" ");
        setTitle(Protocol.APP_NAME + (reg ? " - Crear cuenta" : " - Iniciar sesión"));
        revalidate();
        repaint();
    }

    /** Posición y tamaño de la ventana principal tras iniciar sesión (modo demo). */
    public void setMainBounds(Rectangle r) {
        this.mainBounds = r;
    }

    /** Inicia sesión automáticamente (se usa en el modo demo). */
    public void autoLogin() {
        SwingUtilities.invokeLater(this::submit);
    }

    private void submit() {
        String h = host.getText().trim();
        int p;
        try {
            p = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            status.setText("El puerto debe ser un número.");
            return;
        }
        if (user.getText().isBlank() || pass.getPassword().length == 0) {
            status.setText("Escribe tu usuario y contraseña.");
            return;
        }
        primary.setEnabled(false);
        status.setForeground(Theme.MUTED);
        status.setText("Conectando...");
        Packet req = new Packet(registerMode ? Packet.Type.REGISTER : Packet.Type.LOGIN)
                .put("username", user.getText().trim())
                .put("password", new String(pass.getPassword()))
                .put("displayName", display.getText().trim());

        new SwingWorker<NexoConnection, Void>() {
            @Override
            protected NexoConnection doInBackground() throws Exception {
                if (conn != null && !conn.isClosed()) return conn;
                return NexoConnection.connect(h, p);
            }

            @Override
            protected void done() {
                try {
                    conn = get();
                } catch (Exception e) {
                    fail("No se pudo conectar a " + h + ":" + p + ". ¿Está encendido el servidor?");
                    return;
                }
                conn.setOnClose(() -> fail("Se cerró la conexión con el servidor."));
                conn.setHandler(resp -> {
                    if (resp.getType() == Packet.Type.LOGIN_OK) {
                        MainFrame main = new MainFrame(conn, resp, h, p);
                        if (mainBounds != null) main.setBounds(mainBounds);
                        else main.setLocationRelativeTo(LoginFrame.this);
                        main.setVisible(true);
                        dispose();
                    } else if (resp.getType() == Packet.Type.ERROR) {
                        fail(resp.str("message"));
                    }
                });
                conn.send(req);
            }
        }.execute();
    }

    private void fail(String msg) {
        primary.setEnabled(true);
        status.setForeground(new Color(0xDC2626));
        status.setText("<html><div style='text-align:center;width:300px'>" + msg + "</div></html>");
    }
}
