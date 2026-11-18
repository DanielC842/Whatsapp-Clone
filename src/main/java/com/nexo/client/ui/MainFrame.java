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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Ventana principal: lista de chats a la izquierda y conversación a la derecha. */
public final class MainFrame extends JFrame {

    private final ChatStore store = new ChatStore();
    private final NexoConnection conn;
    private final String host;
    private final int port;

    private final DefaultListModel<Chat> listModel = new DefaultListModel<>();
    private final JList<Chat> chatList = new JList<>(listModel);
    private final JTextField search = Theme.textField("Buscar un chat");
    private final JLabel meAvatar = new JLabel();
    private final JLabel meName = new JLabel();
    private final CardLayout cards = new CardLayout();
    private final JPanel right = new JPanel(cards);
    private final ChatView chatView;
    private boolean updatingList;
    private final Timer refresher;

    public MainFrame(NexoConnection conn, Packet loginOk, String host, int port) {
        super(Protocol.APP_NAME);
        this.conn = conn;
        this.host = host;
        this.port = port;
        store.load(loginOk);
        chatView = new ChatView(store, conn);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(640, 480));
        setSize(1000, 680);
        setIconImage(iconImage());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildRight());
        split.setDividerLocation(330);
        split.setDividerSize(1);
        split.setBorder(null);
        setContentPane(split);

        conn.setHandler(this::handle);
        conn.setOnClose(this::onConnectionLost);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                markCurrentRead();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                refresher.stop();
                boolean anyOpen = false;
                for (Frame f : Frame.getFrames()) if (f != MainFrame.this && f.isDisplayable()) anyOpen = true;
                if (!anyOpen) Notifier.dispose();
                conn.send(new Packet(Packet.Type.LOGOUT));
                conn.close();
            }
        });
        // refresca "escribiendo..." y "últ. vez" periódicamente
        refresher = new Timer(2000, e -> {
            chatList.repaint();
            chatView.refreshHeader();
        });
        refresher.start();

        refreshMe();
        refreshList();
    }

    static Image iconImage() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        Icons.logo(64).paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SIDEBAR);

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(10, 14, 10, 8));
        meName.setFont(Theme.font(Font.BOLD, 16));
        meName.setForeground(Color.WHITE);
        header.add(meAvatar, BorderLayout.WEST);
        header.add(meName, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        JButton newChat = Theme.iconButton(Icons.newChat(22, Color.WHITE), "Nuevo chat");
        newChat.addActionListener(e -> newDirectChat());
        JButton newGroup = Theme.iconButton(Icons.group(24, Color.WHITE), "Nuevo grupo");
        newGroup.addActionListener(e -> newGroup());
        JButton menu = Theme.iconButton(Icons.menu(20, Color.WHITE), "Menú");
        menu.addActionListener(e -> showMenu(menu));
        actions.add(newChat);
        actions.add(newGroup);
        actions.add(menu);
        header.add(actions, BorderLayout.EAST);
        side.add(header, BorderLayout.NORTH);

        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setBackground(Theme.SIDEBAR);
        searchWrap.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.DIVIDER), new EmptyBorder(8, 10, 8, 10)));
        searchWrap.add(search, BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshList(); }
            public void removeUpdate(DocumentEvent e) { refreshList(); }
            public void changedUpdate(DocumentEvent e) { }
        });

        chatList.setCellRenderer(new ChatListRenderer(store));
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatList.setFixedCellHeight(72);
        chatList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingList) return;
            Chat c = chatList.getSelectedValue();
            if (c != null) openChat(c.id);
        });
        JScrollPane sp = new JScrollPane(chatList);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(24);

        JPanel center = new JPanel(new BorderLayout());
        center.add(searchWrap, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        side.add(center, BorderLayout.CENTER);
        side.setMinimumSize(new Dimension(280, 100));
        return side;
    }

    private JComponent buildRight() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setBackground(Theme.CHAT_BG);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel(Icons.logo(110));
        JLabel t1 = new JLabel("Nexo");
        t1.setFont(Theme.font(Font.BOLD, 30));
        t1.setForeground(Theme.TEXT);
        JLabel t2 = new JLabel("Elige un chat o crea uno nuevo para empezar a conversar.");
        t2.setFont(Theme.font(Font.PLAIN, 15));
        t2.setForeground(Theme.MUTED);
        JLabel t3 = new JLabel("Conectado a " + host + ":" + port);
        t3.setFont(Theme.font(Font.PLAIN, 12));
        t3.setForeground(Theme.MUTED);
        for (JComponent c : new JComponent[]{logo, t1, t2, t3}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            box.add(c);
            box.add(Box.createVerticalStrut(10));
        }
        empty.add(box);
        right.add(empty, "empty");
        right.add(chatView, "chat");
        return right;
    }

    private void showMenu(Component anchor) {
        JPopupMenu m = new JPopupMenu();
        JMenuItem profile = new JMenuItem("Editar perfil");
        profile.addActionListener(e -> editProfile());
        JMenuItem logout = new JMenuItem("Cerrar sesión");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame(host, port, null, null).setVisible(true);
        });
        m.add(profile);
        m.add(logout);
        m.show(anchor, 0, anchor.getHeight());
    }

    private void editProfile() {
        JTextField name = Theme.textField("Nombre");
        name.setText(store.me.displayName);
        JTextField about = Theme.textField("Info");
        about.setText(store.me.about);
        final byte[][] photo = {store.me.avatar};
        final boolean[] removed = {false};
        JLabel preview = new JLabel(Theme.userAvatar(store.me, store.me.username, store.me.displayName, 96));
        preview.setAlignmentX(CENTER_ALIGNMENT);
        JButton change = Theme.button("Cambiar foto", false);
        JButton remove = Theme.button("Quitar foto", false);
        change.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Elige tu foto de perfil");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "png", "jpg", "jpeg", "gif", "bmp"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                photo[0] = Theme.toAvatarPng(fc.getSelectedFile());
                removed[0] = false;
                UserInfo tmp = new UserInfo(store.me.username, name.getText(), "", true, 0, photo[0]);
                preview.setIcon(Theme.userAvatar(tmp, tmp.username, tmp.displayName, 96));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo usar esa imagen: " + ex.getMessage());
            }
        });
        remove.addActionListener(e -> {
            photo[0] = null;
            removed[0] = true;
            preview.setIcon(Theme.avatar(store.me.username, name.getText(), 96, false));
        });
        JPanel photoButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        photoButtons.setOpaque(false);
        photoButtons.add(change);
        photoButtons.add(remove);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(preview);
        p.add(Box.createVerticalStrut(8));
        p.add(photoButtons);
        p.add(Box.createVerticalStrut(12));
        for (JComponent c : new JComponent[]{new JLabel("Nombre visible"), name, (JComponent) Box.createVerticalStrut(8), new JLabel("Info"), about}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            p.add(c);
        }
        p.setPreferredSize(new Dimension(320, p.getPreferredSize().height));
        if (JOptionPane.showConfirmDialog(this, p, "Editar perfil", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Packet up = new Packet(Packet.Type.UPDATE_PROFILE)
                    .put("displayName", name.getText()).put("about", about.getText());
            if (removed[0]) up.put("removeAvatar", true);
            else if (photo[0] != null && photo[0] != store.me.avatar) up.put("avatar", photo[0]);
            conn.send(up);
        }
    }

    private void newDirectChat() {
        String user = UserPickerDialog.pickOne(this, store);
        if (user != null) {
            Chat existing = null;
            for (Chat c : store.chats.values()) if (!c.group && c.members.contains(user)) existing = c;
            if (existing != null) openChat(existing.id);
            else conn.send(new Packet(Packet.Type.CREATE_DIRECT).put("username", user));
        }
    }

    private void newGroup() {
        List<String> g = UserPickerDialog.pickGroup(this, store);
        if (g != null) {
            ArrayList<String> members = new ArrayList<>(g.subList(1, g.size()));
            conn.send(new Packet(Packet.Type.CREATE_GROUP).put("name", g.get(0)).put("members", members));
        }
    }

    private void openChat(String chatId) {
        cards.show(right, "chat");
        chatView.open(chatId);
        selectInList(chatId);
        markCurrentRead();
    }

    private void markCurrentRead() {
        String id = chatView.chatId();
        if (id == null || !isActive()) return;
        if (store.unread.getOrDefault(id, 0) > 0) {
            store.unread.remove(id);
            conn.send(new Packet(Packet.Type.MARK_READ).put("chatId", id));
            refreshList();
        }
    }

    // ----------------------------------------------------- paquetes del servidor

    private void handle(Packet p) {
        switch (p.getType()) {
            case USER_UPDATE -> {
                UserInfo u = p.get("user");
                store.users.put(u.username, u);
                if (u.username.equals(store.me.username)) {
                    store.me = u;
                    refreshMe();
                }
                refreshList();
                chatView.refreshHeader();
                if (chatView.chatId() != null) chatView.rebuild(false);
            }
            case CHAT_UPDATE -> {
                Chat c = p.get("chat");
                store.chats.put(c.id, c);
                refreshList();
                if (store.me.username.equals(p.str("openFor"))) openChat(c.id);
            }
            case NEW_MESSAGE -> onNewMessage(p.get("message"));
            case STATUS_UPDATE -> {
                ArrayList<StatusUpdate> ups = p.get("updates");
                boolean current = false;
                for (StatusUpdate s : ups) {
                    store.applyStatus(s);
                    if (s.chatId.equals(chatView.chatId())) current = true;
                }
                if (current) chatView.rebuild(false);
            }
            case TYPING_EVENT -> {
                String chatId = p.str("chatId");
                var map = store.typing.computeIfAbsent(chatId, k -> new java.util.HashMap<>());
                if (p.bool("typing")) map.put(p.str("user"), System.currentTimeMillis());
                else map.remove(p.str("user"));
                chatList.repaint();
                if (chatId.equals(chatView.chatId())) chatView.refreshHeader();
            }
            case ERROR -> JOptionPane.showMessageDialog(this, p.str("message"), "Nexo", JOptionPane.WARNING_MESSAGE);
            default -> { }
        }
    }

    private void onNewMessage(ChatMessage m) {
        store.messagesOf(m.chatId).add(m);
        if (!m.isSystem()) {
            var t = store.typing.get(m.chatId);
            if (t != null) t.remove(m.sender);
        }
        boolean mine = store.me.username.equals(m.sender);
        boolean viewing = m.chatId.equals(chatView.chatId());
        if (!mine && !m.isSystem()) {
            store.unread.merge(m.chatId, 1, Integer::sum);
            if (viewing && isActive()) markCurrentRead();
            else {
                Toolkit.getDefaultToolkit().beep();
                if (!isActive()) {
                    Chat c = store.chats.get(m.chatId);
                    String who = store.displayName(m.sender);
                    String title = (c != null && c.group) ? who + " en " + c.name : who;
                    String body = m.hasFile() ? (m.image ? "Foto" : "Archivo: " + m.fileName) : m.text;
                    Notifier.show(title, body);
                }
            }
        }
        if (viewing) {
            chatView.refreshHeader();
            chatView.rebuild(mine);
        }
        refreshList();
    }

    private void onConnectionLost() {
        if (!isDisplayable()) return;
        int r = JOptionPane.showConfirmDialog(this,
                "Se perdió la conexión con el servidor.\n¿Quieres volver a la pantalla de inicio de sesión?",
                "Nexo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        dispose();
        if (r == JOptionPane.YES_OPTION) new LoginFrame(host, port, null, null).setVisible(true);
    }

    // ----------------------------------------------------- refrescos

    private void refreshMe() {
        meAvatar.setIcon(Theme.userAvatar(store.me, store.me.username, store.me.displayName, 40));
        meName.setText(store.me.displayName);
        setTitle(Protocol.APP_NAME + " - " + store.me.displayName);
    }

    private void refreshList() {
        updatingList = true;
        try {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            String selected = chatView.chatId();
            listModel.clear();
            for (Chat c : store.sortedChats()) {
                if (q.isEmpty() || store.chatTitle(c).toLowerCase(Locale.ROOT).contains(q)) listModel.addElement(c);
            }
            if (selected != null) selectInList(selected);
            int n = store.totalUnread();
            String base = Protocol.APP_NAME + " - " + store.me.displayName;
            setTitle(n > 0 ? "(" + n + ") " + base : base);
        } finally {
            updatingList = false;
        }
    }

    private void selectInList(String chatId) {
        boolean prev = updatingList;
        updatingList = true;
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).id.equals(chatId)) {
                chatList.setSelectedIndex(i);
                chatList.ensureIndexIsVisible(i);
                break;
            }
        }
        updatingList = prev;
    }
}
