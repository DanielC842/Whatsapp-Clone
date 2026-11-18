package com.nexo.client.ui;

import com.nexo.common.ChatMessage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Burbuja de un mensaje (texto, imagen o archivo) con hora y estado. */
final class MessageBubble extends JPanel {

    static final int MAX_TEXT_WIDTH = 340;
    private static final Map<String, ImageIcon> THUMBS = new ConcurrentHashMap<>();

    private final Color fill;
    private final boolean own;

    MessageBubble(ChatMessage m, boolean own, String senderName, ChatMessage.Status status) {
        this.own = own;
        this.fill = own ? Theme.OWN_BUBBLE : Theme.OTHER_BUBBLE;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(6, 10, 5, 10));

        if (senderName != null) {
            JLabel name = new JLabel(senderName);
            name.setFont(Theme.font(Font.BOLD, 13));
            name.setForeground(Theme.avatarColor(m.sender).darker());
            add(left(name));
        }

        if (m.hasFile()) {
            add(left(m.image ? imageComponent(m) : fileComponent(m)));
        }

        if (m.text != null && !m.text.isEmpty()) {
            add(left(textLabel(m.text)));
        }

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        footer.setOpaque(false);
        JLabel time = new JLabel(Theme.time(m.timestamp));
        time.setFont(Theme.font(Font.PLAIN, 11));
        time.setForeground(Theme.MUTED);
        footer.add(time);
        if (own && status != null) {
            Icon ticks = switch (status) {
                case SENT -> Icons.ticks(1, Theme.MUTED);
                case DELIVERED -> Icons.ticks(2, Theme.MUTED);
                case READ -> Icons.ticks(2, Theme.READ);
            };
            JLabel t = new JLabel(ticks);
            t.setToolTipText(switch (status) {
                case SENT -> "Enviado";
                case DELIVERED -> "Entregado";
                case READ -> "Leído";
            });
            footer.add(t);
        }
        add(left(footer));
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    /** Etiqueta de texto que se parte en varias líneas si es más ancha que MAX_TEXT_WIDTH. */
    private static JLabel textLabel(String text) {
        Font f = Theme.font(Font.PLAIN, 15);
        JLabel l = new JLabel("<html>" + Theme.toHtml(text, f) + "</html>");
        l.setFont(f);
        l.setForeground(Theme.TEXT);
        View v = (View) l.getClientProperty(BasicHTML.propertyKey);
        if (v != null && v.getPreferredSpan(View.X_AXIS) > MAX_TEXT_WIDTH) {
            v.setSize(MAX_TEXT_WIDTH, 0);
            int h = (int) Math.ceil(v.getPreferredSpan(View.Y_AXIS));
            Dimension d = new Dimension(MAX_TEXT_WIDTH, h);
            l.setPreferredSize(d);
            l.setMaximumSize(d);
        }
        return l;
    }

    private JComponent imageComponent(ChatMessage m) {
        ImageIcon icon = THUMBS.computeIfAbsent(m.id, k -> {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(m.fileData));
                if (img == null) return null;
                double scale = Math.min(1.0, Math.min(260.0 / img.getWidth(), 260.0 / img.getHeight()));
                int w = Math.max(1, (int) (img.getWidth() * scale));
                int h = Math.max(1, (int) (img.getHeight() * scale));
                return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                return null;
            }
        });
        if (icon == null) return fileComponent(m);
        JLabel l = new JLabel(icon);
        l.setBorder(new EmptyBorder(2, 0, 4, 0));
        l.setToolTipText("Clic para ver en grande");
        Theme.onClick(l, () -> showFullImage(m));
        return l;
    }

    private JComponent fileComponent(ChatMessage m) {
        JLabel l = new JLabel("<html><b>" + Theme.toHtml(m.fileName, Theme.font(Font.BOLD, 14)) + "</b><br>"
                + "<span style='color:#6B7280'>" + Theme.fileSize(m.fileData.length) + " · clic para guardar</span></html>",
                Icons.file(30, Theme.PRIMARY), SwingConstants.LEFT);
        l.setIconTextGap(8);
        l.setBorder(new EmptyBorder(4, 2, 4, 6));
        Theme.onClick(l, () -> saveFile(m));
        return l;
    }

    private void showFullImage(ChatMessage m) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(m.fileData));
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog d = new JDialog(owner, m.fileName, Dialog.ModalityType.MODELESS);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            double scale = Math.min(1.0, Math.min(screen.width * 0.8 / img.getWidth(), screen.height * 0.8 / img.getHeight()));
            Image shown = img.getScaledInstance((int) (img.getWidth() * scale), (int) (img.getHeight() * scale), Image.SCALE_SMOOTH);
            JLabel l = new JLabel(new ImageIcon(shown));
            l.setOpaque(true);
            l.setBackground(Color.BLACK);
            JButton save = Theme.button("Guardar imagen", true);
            save.addActionListener(e -> saveFile(m));
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.add(save);
            d.add(l, BorderLayout.CENTER);
            d.add(bottom, BorderLayout.SOUTH);
            d.pack();
            d.setLocationRelativeTo(owner);
            d.setVisible(true);
        } catch (Exception e) {
            saveFile(m);
        }
    }

    private void saveFile(ChatMessage m) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(m.fileName));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fc.getSelectedFile().toPath(), m.fileData);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar: " + e.getMessage());
            }
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 18));
        g.fill(new RoundRectangle2D.Float(0, 1, getWidth(), getHeight() - 1, 16, 16));
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() - 1, 16, 16));
        // esquina "recta" del lado del remitente
        if (own) g.fillRect(getWidth() - 10, 0, 10, 10);
        else g.fillRect(0, 0, 10, 10);
        g.dispose();
        super.paintComponent(g0);
    }
}
