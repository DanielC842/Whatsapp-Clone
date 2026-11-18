package com.nexo.client.ui;

import com.nexo.client.ChatStore;
import com.nexo.client.NexoConnection;
import com.nexo.common.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/** Panel derecho: cabecera del chat, lista de mensajes y caja para escribir. */
final class ChatView extends JPanel {

    private final ChatStore store;
    private final NexoConnection conn;
    private String chatId;

    private final JLabel avatar = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final MessagesPanel messages = new MessagesPanel();
    private final JScrollPane scroll;
    private final MessageInput input = new MessageInput("Escribe un mensaje");

    private boolean typingSent;
    private final Timer typingStop;

    ChatView(ChatStore store, NexoConnection conn) {
        super(new BorderLayout());
        this.store = store;
        this.conn = conn;

        // Cabecera
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Theme.PRIMARY_LIGHT);
        header.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.DIVIDER), new EmptyBorder(10, 16, 10, 16)));
        title.setFont(Theme.font(Font.BOLD, 16));
        title.setForeground(Theme.TEXT);
        subtitle.setFont(Theme.font(Font.PLAIN, 13));
        subtitle.setForeground(Theme.MUTED);
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        names.add(title);
        names.add(subtitle);
        header.add(avatar, BorderLayout.WEST);
        header.add(names, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Mensajes
        scroll = new JScrollPane(messages);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().setBackground(Theme.CHAT_BG);
        add(scroll, BorderLayout.CENTER);

        // Caja de texto
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(Theme.PRIMARY_LIGHT);
        bar.setBorder(new EmptyBorder(10, 12, 10, 12));
        JButton emoji = Theme.iconButton(Icons.smiley(24, Theme.MUTED), "Emojis");
        EmojiPicker picker = new EmojiPicker(input);
        emoji.addActionListener(e -> picker.showAbove(emoji));
        JButton attach = Theme.iconButton(Icons.attach(24, Theme.MUTED), "Adjuntar imagen o archivo (o arrástralo aquí)");
        attach.addActionListener(e -> attachFile());
        JButton send = Theme.iconButton(Icons.send(22, Theme.PRIMARY), "Enviar (Enter)");
        send.addActionListener(e -> sendText());
        input.setOnSend(this::sendText);
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(emoji);
        leftButtons.add(attach);
        JPanel sendWrap = new JPanel(new BorderLayout());
        sendWrap.setOpaque(false);
        sendWrap.add(send, BorderLayout.SOUTH);
        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setOpaque(false);
        leftWrap.add(leftButtons, BorderLayout.SOUTH);
        bar.add(leftWrap, BorderLayout.WEST);
        bar.add(input, BorderLayout.CENTER);
        bar.add(sendWrap, BorderLayout.EAST);

        // Arrastrar y soltar archivos sobre el chat para enviarlos
        TransferHandler drop = new FileDropHandler(this::sendFiles);
        setTransferHandler(drop);
        messages.setTransferHandler(drop);
        scroll.setTransferHandler(drop);
        add(bar, BorderLayout.SOUTH);

        typingStop = new Timer(2500, e -> setTyping(false));
        typingStop.setRepeats(false);
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTyped();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTyped();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    String chatId() {
        return chatId;
    }

    void open(String chatId) {
        if (!chatId.equals(this.chatId)) {
            setTyping(false);
            input.setText("");
        }
        this.chatId = chatId;
        refreshHeader();
        rebuild(true);
        input.requestFocusInWindow();
    }

    void refreshHeader() {
        if (chatId == null) return;
        Chat c = store.chats.get(chatId);
        if (c == null) return;
        String name = store.chatTitle(c);
        if (c.group) avatar.setIcon(Theme.avatar(c.id, name, 42, true));
        else {
            String other = c.otherMember(store.me.username);
            avatar.setIcon(Theme.userAvatar(store.users.get(other), other, name, 42));
        }
        title.setText(name);

        List<String> typing = store.typingIn(chatId);
        if (!typing.isEmpty()) {
            subtitle.setForeground(Theme.PRIMARY);
            if (c.group) subtitle.setText(firstName(store.displayName(typing.get(0))) + " está escribiendo...");
            else subtitle.setText("escribiendo...");
            return;
        }
        subtitle.setForeground(Theme.MUTED);
        if (c.group) {
            StringBuilder sb = new StringBuilder();
            for (String m : c.members) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(m.equals(store.me.username) ? "Tú" : firstName(store.displayName(m)));
            }
            subtitle.setText(sb.toString());
        } else {
            UserInfo u = store.users.get(c.otherMember(store.me.username));
            if (u == null) subtitle.setText("");
            else if (u.online) {
                subtitle.setText("en línea");
                subtitle.setForeground(Theme.ONLINE.darker());
            } else subtitle.setText(Theme.lastSeen(u.lastSeen));
        }
    }

    private static String firstName(String n) {
        int i = n.indexOf(' ');
        return i > 0 ? n.substring(0, i) : n;
    }

    /** Vuelve a dibujar todos los mensajes del chat abierto. */
    void rebuild(boolean forceBottom) {
        Chat c = store.chats.get(chatId);
        if (c == null) return;
        JScrollBar bar = scroll.getVerticalScrollBar();
        boolean atBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 40;

        messages.removeAll();
        messages.add(Box.createVerticalGlue());
        long lastDay = -1;
        for (ChatMessage m : store.messagesOf(chatId)) {
            if (lastDay < 0 || !Theme.sameDay(lastDay, m.timestamp)) {
                messages.add(centered(Theme.dayLabel(m.timestamp), Color.WHITE));
                lastDay = m.timestamp;
            }
            if (m.isSystem()) {
                messages.add(centered(m.text, Theme.SYSTEM_PILL));
                continue;
            }
            boolean own = m.sender.equals(store.me.username);
            String sender = (c.group && !own) ? store.displayName(m.sender) : null;
            MessageBubble b = new MessageBubble(m, own, sender, own ? m.statusFor(c.members) : null);
            JPanel row = new JPanel(new FlowLayout(own ? FlowLayout.RIGHT : FlowLayout.LEFT, 14, 3));
            row.setOpaque(false);
            row.add(b);
            messages.add(fixHeight(row));
        }
        messages.add(Box.createVerticalStrut(8));
        messages.revalidate();
        messages.repaint();
        if (atBottom || forceBottom) scrollToBottom();
    }

    private static JComponent fixHeight(JComponent row) {
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent centered(String text, Color bg) {
        JLabel l = new JLabel("<html>" + Theme.toHtml(text, Theme.font(Font.PLAIN, 12)) + "</html>") {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(bg);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g.dispose();
                super.paintComponent(g0);
            }
        };
        l.setFont(Theme.font(Font.PLAIN, 12));
        l.setForeground(Theme.MUTED);
        l.setBorder(new EmptyBorder(4, 10, 4, 10));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        row.setOpaque(false);
        row.add(l);
        return fixHeight(row);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            messages.validate();
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ----------------------------------------------------------- acciones

    private void sendText() {
        String text = input.getText();
        if (chatId == null || text.isBlank()) return;
        conn.send(new Packet(Packet.Type.SEND_MESSAGE).put("chatId", chatId).put("text", text));
        input.setText("");
        setTyping(false);
    }

    private void attachFile() {
        if (chatId == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Enviar imagen o archivo");
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        sendFiles(java.util.Arrays.asList(fc.getSelectedFiles()));
    }

    /** Envía uno o varios archivos al chat abierto (el texto escrito va como pie del primero). */
    private void sendFiles(List<File> files) {
        if (chatId == null || files.isEmpty()) return;
        String caption = input.getText().trim();
        boolean captionUsed = false;
        for (File f : files) {
            if (!f.isFile()) continue;
            if (f.length() > Protocol.MAX_FILE_BYTES) {
                JOptionPane.showMessageDialog(this, "\"" + f.getName() + "\" pesa más de "
                        + Theme.fileSize(Protocol.MAX_FILE_BYTES) + " y no se puede enviar.");
                continue;
            }
            try {
                byte[] data = Files.readAllBytes(f.toPath());
                String n = f.getName().toLowerCase(Locale.ROOT);
                boolean image = n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".gif") || n.endsWith(".bmp");
                conn.send(new Packet(Packet.Type.SEND_MESSAGE).put("chatId", chatId)
                        .put("text", captionUsed ? "" : caption)
                        .put("fileName", f.getName()).put("fileData", data).put("image", image));
                captionUsed = true;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo leer " + f.getName() + ": " + ex.getMessage());
            }
        }
        if (captionUsed) input.setText("");
    }

    /** Recibe archivos arrastrados desde el explorador. */
    private static final class FileDropHandler extends TransferHandler {
        private final java.util.function.Consumer<List<File>> sink;

        FileDropHandler(java.util.function.Consumer<List<File>> sink) {
            this.sink = sink;
        }

        @Override
        public boolean canImport(TransferSupport s) {
            return s.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport s) {
            if (!canImport(s)) return false;
            try {
                List<File> files = (List<File>) s.getTransferable()
                        .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                SwingUtilities.invokeLater(() -> sink.accept(files));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void onTyped() {
        if (input.getText().isEmpty()) {
            setTyping(false);
            return;
        }
        if (!typingSent) setTyping(true);
        typingStop.restart();
    }

    private void setTyping(boolean typing) {
        if (chatId == null || typing == typingSent) return;
        typingSent = typing;
        conn.send(new Packet(Packet.Type.TYPING).put("chatId", chatId).put("typing", typing));
    }

    /** Panel de mensajes que ocupa el ancho visible y se pega abajo si hay pocos mensajes. */
    private static final class MessagesPanel extends JPanel implements Scrollable {
        MessagesPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CHAT_BG);
            setBorder(new EmptyBorder(6, 0, 6, 0));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height - 40;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport v && v.getHeight() > getPreferredSize().height;
        }
    }
}
