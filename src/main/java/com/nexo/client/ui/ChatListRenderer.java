package com.nexo.client.ui;

import com.nexo.client.ChatStore;
import com.nexo.common.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

/** Dibuja cada fila de la lista de chats: avatar, nombre, último mensaje, hora y no leídos. */
final class ChatListRenderer extends JPanel implements ListCellRenderer<Chat> {

    private final ChatStore store;
    private final JLabel avatar = new JLabel();
    private final JLabel name = new JLabel();
    private final JLabel preview = new JLabel();
    private final JLabel stamp = new JLabel();
    private final JLabel badge = new JLabel() {
        @Override
        protected void paintComponent(Graphics g0) {
            if (!getText().isEmpty()) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.PRIMARY);
                g.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g.dispose();
            }
            super.paintComponent(g0);
        }
    };
    private boolean onlineDot;
    private int availableWidth = 330;

    ChatListRenderer(ChatStore store) {
        super(new BorderLayout(12, 0));
        this.store = store;
        setBorder(new EmptyBorder(10, 14, 10, 14));
        name.setFont(Theme.font(Font.BOLD, 15));
        name.setForeground(Theme.TEXT);
        preview.setFont(Theme.font(Font.PLAIN, 13));
        preview.setForeground(Theme.MUTED);
        stamp.setFont(Theme.font(Font.PLAIN, 12));
        badge.setFont(Theme.font(Font.BOLD, 11));
        badge.setForeground(Color.WHITE);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setBorder(new EmptyBorder(1, 6, 1, 6));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(name, BorderLayout.CENTER);
        top.add(stamp, BorderLayout.EAST);
        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setOpaque(false);
        bottom.add(preview, BorderLayout.CENTER);
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        bottom.add(badgeWrap, BorderLayout.EAST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(top);
        text.add(bottom);
        add(avatar, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Chat> list, Chat c, int index,
                                                  boolean selected, boolean focus) {
        availableWidth = list.getWidth();
        String title = store.chatTitle(c);
        String key = c.group ? c.id : c.otherMember(store.me.username);
        avatar.setIcon(Theme.avatar(key, title, 48, c.group));
        name.setText(title);
        UserInfo other = c.group ? null : store.users.get(key);
        onlineDot = other != null && other.online;

        List<String> typing = store.typingIn(c.id);
        ChatMessage last = store.lastMessage(c.id);
        if (!typing.isEmpty()) {
            preview.setForeground(Theme.PRIMARY);
            preview.setText("escribiendo...");
        } else {
            preview.setForeground(Theme.MUTED);
            preview.setText(last == null ? (c.group ? "Grupo creado" : "Toca para chatear") : previewText(c, last));
        }
        stamp.setText(last == null ? "" : Theme.shortStamp(last.timestamp));
        int unread = store.unread.getOrDefault(c.id, 0);
        badge.setText(unread > 0 ? String.valueOf(unread) : "");
        badge.setVisible(unread > 0);
        stamp.setForeground(unread > 0 ? Theme.PRIMARY : Theme.MUTED);
        setBackground(selected ? Theme.SELECTED : Theme.SIDEBAR);
        return this;
    }

    private String previewText(Chat c, ChatMessage m) {
        String body;
        if (m.hasFile()) body = (m.image ? "Foto" : "Archivo: " + m.fileName) + (m.text.isEmpty() ? "" : " · " + m.text);
        else body = m.text;
        body = body.replace('\n', ' ');
        String prefix = "";
        if (!m.isSystem()) {
            if (m.sender.equals(store.me.username)) prefix = "Tú: ";
            else if (c.group) prefix = store.displayName(m.sender).split(" ")[0] + ": ";
        }
        return "<html><nobr>" + Theme.toHtml(ellipsize(prefix + body), preview.getFont()) + "</nobr></html>";
    }

    /** Recorta el texto con "..." para que quepa en una sola línea. */
    private String ellipsize(String s) {
        int max = Math.max(60, availableWidth - 110);
        FontMetrics fm = preview.getFontMetrics(preview.getFont());
        if (fm.stringWidth(s) <= max) return s;
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end) + "...") > max) end--;
        if (end > 0 && Character.isHighSurrogate(s.charAt(end - 1))) end--;
        return s.substring(0, end) + "...";
    }

    @Override
    protected void paintChildren(Graphics g0) {
        super.paintChildren(g0);
        if (onlineDot) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = avatar.getX() + 36, y = avatar.getY() + avatar.getHeight() / 2 + 12;
            g.setColor(getBackground());
            g.fill(new Ellipse2D.Float(x - 2, y - 2, 16, 16));
            g.setColor(Theme.ONLINE);
            g.fill(new Ellipse2D.Float(x, y, 12, 12));
            g.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(10, d.height);
    }
}
