package com.nexo.client.ui;

import com.nexo.client.ChatStore;
import com.nexo.common.UserInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Diálogo para elegir contactos: uno (nuevo chat) o varios (nuevo grupo). */
final class UserPickerDialog extends JDialog {

    private final List<JToggleButton> boxes = new ArrayList<>();
    private final List<String> usernames = new ArrayList<>();
    private final JTextField groupName = Theme.textField("Nombre del grupo");
    private List<String> result;
    private String resultGroupName;

    private UserPickerDialog(Window owner, ChatStore store, boolean group) {
        super(owner, group ? "Nuevo grupo" : "Nuevo chat", ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel head = new JLabel(group ? "Elige el nombre y los participantes" : "Elige con quién quieres hablar");
        head.setFont(Theme.font(Font.BOLD, 15));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        if (group) top.add(groupName, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        List<UserInfo> users = new ArrayList<>(store.users.values());
        users.removeIf(u -> u.username.equals(store.me.username));
        users.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        ButtonGroup single = new ButtonGroup();
        for (UserInfo u : users) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(6, 4, 6, 4));
            JLabel l = new JLabel("<html><b>" + Theme.toHtml(u.displayName, Theme.font(Font.BOLD, 14)) + "</b><br><span style='color:#6B7280'>@"
                    + u.username + " · " + (u.online ? "en línea" : Theme.toHtml(u.about, Theme.font(Font.PLAIN, 14))) + "</span></html>",
                    Theme.userAvatar(u, u.username, u.displayName, 38), SwingConstants.LEFT);
            l.setIconTextGap(10);
            JToggleButton cb = group ? new JCheckBox() : new JRadioButton();
            if (!group) single.add(cb);
            cb.setOpaque(false);
            row.add(l, BorderLayout.CENTER);
            row.add(cb, BorderLayout.EAST);
            Theme.onClick(l, () -> {
                cb.setSelected(!cb.isSelected() || !group);
            });
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            list.add(row);
            boxes.add(cb);
            usernames.add(u.username);
        }
        if (users.isEmpty()) list.add(new JLabel("Todavía no hay otros usuarios registrados."));
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(380, 320));
        root.add(sp, BorderLayout.CENTER);

        JButton ok = Theme.button(group ? "Crear grupo" : "Abrir chat", true);
        JButton cancel = Theme.button("Cancelar", false);
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            List<String> sel = new ArrayList<>();
            for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isSelected()) sel.add(usernames.get(i));
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Selecciona al menos un contacto.");
                return;
            }
            if (group && groupName.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Escribe un nombre para el grupo.");
                return;
            }
            result = sel;
            resultGroupName = groupName.getText().trim();
            dispose();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Devuelve el usuario elegido o null. */
    static String pickOne(Window owner, ChatStore store) {
        UserPickerDialog d = new UserPickerDialog(owner, store, false);
        d.setVisible(true);
        return d.result == null ? null : d.result.get(0);
    }

    /** Devuelve [nombreGrupo, miembro1, miembro2...] o null. */
    static List<String> pickGroup(Window owner, ChatStore store) {
        UserPickerDialog d = new UserPickerDialog(owner, store, true);
        d.setVisible(true);
        if (d.result == null) return null;
        List<String> out = new ArrayList<>();
        out.add(d.resultGroupName);
        out.addAll(d.result);
        return out;
    }
}
