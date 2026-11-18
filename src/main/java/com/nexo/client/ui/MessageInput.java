package com.nexo.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Caja de texto para escribir mensajes. A diferencia de un JTextField, muestra bien
 * los emojis: cada emoji se dibuja con la fuente de emojis del sistema.
 * Enter envía; Shift+Enter hace un salto de línea.
 */
final class MessageInput extends JTextPane {

    private final String placeholder;
    private Runnable onSend = () -> { };
    private boolean restyling;

    MessageInput(String placeholder) {
        this.placeholder = placeholder;
        Font base = Theme.font(Font.PLAIN, 15);
        setFont(base);
        setForeground(Theme.TEXT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.DIVIDER, 1, true), new EmptyBorder(8, 10, 8, 10)));

        InputMap im = getInputMap(WHEN_FOCUSED);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nexo-send");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.insertBreakAction);
        am.put("nexo-send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSend.run();
            }
        });

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(MessageInput.this::restyle);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                repaint();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                repaint();
            }
        });
    }

    void setOnSend(Runnable r) {
        this.onSend = r;
    }

    /** Inserta texto (por ejemplo un emoji) donde está el cursor. */
    void insertAtCaret(String s) {
        replaceSelection(s);
        requestFocusInWindow();
    }

    /** Aplica la fuente de emojis a los caracteres que la necesitan. */
    private void restyle() {
        if (restyling) return;
        restyling = true;
        try {
            StyledDocument doc = getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            Font base = getFont();
            SimpleAttributeSet normal = new SimpleAttributeSet();
            StyleConstants.setFontFamily(normal, base.getFamily());
            SimpleAttributeSet emoji = new SimpleAttributeSet();
            StyleConstants.setFontFamily(emoji, Theme.emojiFont(base.getSize()).getFamily());
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                int len = Character.charCount(cp);
                doc.setCharacterAttributes(i, len, Theme.needsEmojiFont(base, cp) ? emoji : normal, false);
                i += len;
            }
        } catch (BadLocationException ignored) {
        } finally {
            restyling = false;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // crece hasta ~5 líneas, luego se queda fijo
        int line = getFontMetrics(getFont()).getHeight();
        Insets in = getInsets();
        int min = line + in.top + in.bottom;
        int max = line * 5 + in.top + in.bottom;
        return new Dimension(Math.max(60, d.width), Math.max(min, Math.min(max, d.height)));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (getDocument().getLength() == 0 && !isFocusOwner()) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Theme.MUTED);
            g.setFont(getFont());
            Insets in = getInsets();
            g.drawString(placeholder, in.left, in.top + g.getFontMetrics().getAscent());
            g.dispose();
        }
    }
}
