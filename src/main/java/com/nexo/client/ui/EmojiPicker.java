package com.nexo.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Menú emergente con emojis frecuentes; al elegir uno se inserta en la caja de texto. */
final class EmojiPicker extends JPopupMenu {

    private static final String[] EMOJIS = {
            "😀", "😂", "🤣", "😊", "😍", "😘", "😎", "🤔",
            "😅", "😉", "😢", "😭", "😡", "😱", "🥳", "😴",
            "👍", "👎", "👏", "🙌", "🙏", "💪", "👋", "🤝",
            "❤", "💔", "🔥", "✨", "🎉", "💯", "✅", "❌",
            "📚", "💻", "☕", "🍕", "⚽", "🎵", "📌", "⏰"
    };

    EmojiPicker(MessageInput target) {
        setBorder(BorderFactory.createLineBorder(Theme.DIVIDER));
        JPanel grid = new JPanel(new GridLayout(0, 8, 2, 2));
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));
        Font f = Theme.emojiFont(22);
        for (String e : EMOJIS) {
            JButton b = new JButton(e);
            b.setFont(f.canDisplayUpTo(e) == -1 ? f : Theme.font(Font.PLAIN, 20));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setPreferredSize(new Dimension(46, 42));
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addChangeListener(ev -> {
                b.setOpaque(b.getModel().isRollover());
                b.setBackground(Theme.SELECTED);
            });
            b.addActionListener(ev -> {
                setVisible(false);
                target.insertAtCaret(e);
                SwingUtilities.invokeLater(target::requestFocusInWindow);
            });
            grid.add(b);
        }
        add(grid);
    }

    void showAbove(Component anchor) {
        Dimension d = getPreferredSize();
        show(anchor, 0, -d.height - 4);
    }
}
