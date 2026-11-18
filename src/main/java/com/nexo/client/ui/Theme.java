package com.nexo.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.*;

/** Colores, fuentes y utilidades visuales de Nexo. */
public final class Theme {

    public static final Color PRIMARY = new Color(0x4F46E5);
    public static final Color PRIMARY_DARK = new Color(0x3730A3);
    public static final Color PRIMARY_LIGHT = new Color(0xEEF2FF);
    public static final Color CHAT_BG = new Color(0xF1F2F8);
    public static final Color OWN_BUBBLE = new Color(0xDDE3FF);
    public static final Color OTHER_BUBBLE = Color.WHITE;
    public static final Color SIDEBAR = Color.WHITE;
    public static final Color SELECTED = new Color(0xE8EAF6);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color DIVIDER = new Color(0xE5E7EB);
    public static final Color READ = new Color(0x0EA5E9);
    public static final Color ONLINE = new Color(0x22C55E);
    public static final Color SYSTEM_PILL = new Color(0xFFF7D6);

    private static final Color[] AVATAR_COLORS = {
            new Color(0xEF4444), new Color(0xF97316), new Color(0xEAB308), new Color(0x22C55E),
            new Color(0x14B8A6), new Color(0x3B82F6), new Color(0x8B5CF6), new Color(0xEC4899)
    };

    private static final String BASE_FAMILY;
    private static final String EMOJI_FAMILY;

    static {
        Set<String> fams = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        BASE_FAMILY = fams.contains("Segoe UI") ? "Segoe UI" : Font.SANS_SERIF;
        String emoji = Font.DIALOG;
        for (String f : new String[]{"Segoe UI Emoji", "Noto Color Emoji", "Apple Color Emoji", "Segoe UI Symbol"}) {
            if (fams.contains(f)) {
                emoji = f;
                break;
            }
        }
        EMOJI_FAMILY = emoji;
    }

    private Theme() {
    }

    public static Font font(int style, float size) {
        return new Font(BASE_FAMILY, style, Math.round(size));
    }

    public static void installDefaults() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Font f = font(Font.PLAIN, 14);
        for (String k : new String[]{"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "CheckBox.font", "List.font", "OptionPane.messageFont", "OptionPane.buttonFont",
                "MenuItem.font", "Menu.font", "TextArea.font", "ToolTip.font", "TitledBorder.font"}) {
            UIManager.put(k, f);
        }
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("CheckBox.focus", new Color(0, 0, 0, 0));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
    }

    public static Color avatarColor(String key) {
        return AVATAR_COLORS[Math.floorMod(key == null ? 0 : key.hashCode(), AVATAR_COLORS.length)];
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(parts[0].codePointAt(0));
        if (parts.length > 1) sb.appendCodePoint(parts[parts.length - 1].codePointAt(0));
        return sb.toString().toUpperCase();
    }

    /** Avatar circular con iniciales; los grupos van en cuadrado redondeado. */
    public static Icon avatar(String key, String name, int size, boolean group) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(avatarColor(key));
                if (group) g.fill(new RoundRectangle2D.Float(x, y, size, size, size * 0.45f, size * 0.45f));
                else g.fillOval(x, y, size, size);
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, size * 0.38f));
                String s = initials(name);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** Botón plano con esquinas redondeadas. */
    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = primary ? PRIMARY : PRIMARY_LIGHT;
                if (getModel().isRollover()) bg = primary ? PRIMARY_DARK : SELECTED;
                if (!isEnabled()) bg = DIVIDER;
                g.setColor(bg);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setForeground(primary ? Color.WHITE : PRIMARY_DARK);
        b.setFont(font(Font.BOLD, 14));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Botón redondo con un icono dibujado. */
    public static JButton iconButton(Icon icon, String tooltip) {
        JButton b = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g0) {
                if (getModel().isRollover()) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(0, 0, 0, 20));
                    g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    g.dispose();
                }
                super.paintComponent(g0);
            }
        };
        b.setToolTipText(tooltip);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setPreferredSize(new Dimension(38, 38));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JTextField textField(String placeholder) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) paintPlaceholder(this, g, placeholder);
            }
        };
        styleField(f);
        return f;
    }

    public static JPasswordField passwordField(String placeholder) {
        JPasswordField f = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !isFocusOwner()) paintPlaceholder(this, g, placeholder);
            }
        };
        styleField(f);
        return f;
    }

    private static void styleField(JTextField f) {
        f.setFont(font(Font.PLAIN, 15));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                f.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                f.repaint();
            }
        });
    }

    private static void paintPlaceholder(JTextField f, Graphics g0, String text) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(MUTED);
        g.setFont(f.getFont());
        Insets in = f.getInsets();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, in.left, (f.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    public static void onClick(JComponent c, Runnable r) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                r.run();
            }
        });
    }

    // ------------------------------------------------------------ texto

    /** Convierte texto plano a HTML seguro, usando la fuente de emojis donde haga falta. */
    public static String toHtml(String text, Font base) {
        StringBuilder sb = new StringBuilder();
        boolean inEmoji = false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            boolean needsEmoji = cp > 0x2000 && !base.canDisplay(cp);
            if (needsEmoji != inEmoji) {
                sb.append(needsEmoji ? "<font face='" + EMOJI_FAMILY + "'>" : "</font>");
                inEmoji = needsEmoji;
            }
            switch (cp) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\n' -> sb.append("<br>");
                default -> sb.appendCodePoint(cp);
            }
        }
        if (inEmoji) sb.append("</font>");
        return sb.toString();
    }

    // ------------------------------------------------------------ fechas

    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM/yyyy");
    private static final String[] DAYS = {"domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"};

    public static String time(long ts) {
        return TIME.format(new Date(ts));
    }

    private static long dayStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static boolean sameDay(long a, long b) {
        return dayStart(a) == dayStart(b);
    }

    /** "Hoy", "Ayer", el día de la semana o la fecha. */
    public static String dayLabel(long ts) {
        long today = dayStart(System.currentTimeMillis());
        long d = dayStart(ts);
        long diffDays = Math.round((today - d) / 86_400_000.0);
        if (diffDays == 0) return "Hoy";
        if (diffDays == 1) return "Ayer";
        if (diffDays < 7) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(ts);
            String day = DAYS[c.get(Calendar.DAY_OF_WEEK) - 1];
            return Character.toUpperCase(day.charAt(0)) + day.substring(1);
        }
        return DATE.format(new Date(ts));
    }

    /** Hora corta para la lista de chats. */
    public static String shortStamp(long ts) {
        String d = dayLabel(ts);
        return d.equals("Hoy") ? time(ts) : d;
    }

    public static String lastSeen(long ts) {
        if (ts <= 0) return "desconectado";
        String d = dayLabel(ts);
        if (d.equals("Hoy")) return "últ. vez hoy a las " + time(ts);
        if (d.equals("Ayer")) return "últ. vez ayer a las " + time(ts);
        return "últ. vez el " + d.toLowerCase() + " a las " + time(ts);
    }

    public static String fileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
