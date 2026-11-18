package com.nexo.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Iconos dibujados con Java2D (así no dependemos de imágenes externas). */
public final class Icons {

    private Icons() {
    }

    private abstract static class Painted implements Icon {
        final int size;
        final Color color;

        Painted(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public final void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            g.setColor(color);
            paint(g, size);
            g.dispose();
        }

        abstract void paint(Graphics2D g, float s);

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Avión de papel para "enviar". */
    public static Icon send(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                Path2D p = new Path2D.Float();
                p.moveTo(s * 0.08, s * 0.12);
                p.lineTo(s * 0.95, s * 0.5);
                p.lineTo(s * 0.08, s * 0.88);
                p.lineTo(s * 0.2, s * 0.5);
                p.closePath();
                g.fill(p);
            }
        };
    }

    /** Clip para adjuntar archivos. */
    public static Icon attach(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                g.setStroke(new BasicStroke(s * 0.09f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.rotate(Math.toRadians(40), s / 2, s / 2);
                Path2D p = new Path2D.Float();
                p.moveTo(s * 0.62, s * 0.3);
                p.lineTo(s * 0.62, s * 0.75);
                p.curveTo(s * 0.62, s * 0.95, s * 0.36, s * 0.95, s * 0.36, s * 0.75);
                p.lineTo(s * 0.36, s * 0.2);
                p.curveTo(s * 0.36, s * 0.05, s * 0.54, s * 0.05, s * 0.54, s * 0.2);
                p.lineTo(s * 0.54, s * 0.68);
                p.curveTo(s * 0.54, s * 0.78, s * 0.45, s * 0.78, s * 0.45, s * 0.68);
                p.lineTo(s * 0.45, s * 0.3);
                g.draw(p);
            }
        };
    }

    /** Lápiz/burbuja con "+" para nuevo chat. */
    public static Icon newChat(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                g.setStroke(new BasicStroke(s * 0.08f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                RoundRectangle2D r = new RoundRectangle2D.Float(s * 0.1f, s * 0.12f, s * 0.8f, s * 0.62f, s * 0.3f, s * 0.3f);
                g.draw(r);
                Path2D tail = new Path2D.Float();
                tail.moveTo(s * 0.28, s * 0.74);
                tail.lineTo(s * 0.22, s * 0.92);
                tail.lineTo(s * 0.45, s * 0.74);
                g.draw(tail);
                g.draw(new Line2D.Float(s * 0.5f, s * 0.28f, s * 0.5f, s * 0.58f));
                g.draw(new Line2D.Float(s * 0.35f, s * 0.43f, s * 0.65f, s * 0.43f));
            }
        };
    }

    /** Tres personas: nuevo grupo. */
    public static Icon group(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                g.fill(new Ellipse2D.Float(s * 0.36f, s * 0.12f, s * 0.28f, s * 0.28f));
                g.fill(new Arc2D.Float(s * 0.24f, s * 0.44f, s * 0.52f, s * 0.6f, 0, 180, Arc2D.CHORD));
                g.fill(new Ellipse2D.Float(s * 0.06f, s * 0.24f, s * 0.2f, s * 0.2f));
                g.fill(new Arc2D.Float(s * 0.0f, s * 0.5f, s * 0.32f, s * 0.4f, 0, 180, Arc2D.CHORD));
                g.fill(new Ellipse2D.Float(s * 0.74f, s * 0.24f, s * 0.2f, s * 0.2f));
                g.fill(new Arc2D.Float(s * 0.68f, s * 0.5f, s * 0.32f, s * 0.4f, 0, 180, Arc2D.CHORD));
            }
        };
    }

    /** Tres puntos verticales (menú). */
    public static Icon menu(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                float d = s * 0.16f;
                for (int i = 0; i < 3; i++) g.fill(new Ellipse2D.Float((s - d) / 2, s * 0.12f + i * s * 0.3f, d, d));
            }
        };
    }

    /** Lupa. */
    public static Icon search(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                g.setStroke(new BasicStroke(s * 0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Float(s * 0.12f, s * 0.12f, s * 0.5f, s * 0.5f));
                g.draw(new Line2D.Float(s * 0.55f, s * 0.55f, s * 0.85f, s * 0.85f));
            }
        };
    }

    /** Documento para archivos adjuntos. */
    public static Icon file(int size, Color color) {
        return new Painted(size, color) {
            @Override
            void paint(Graphics2D g, float s) {
                Path2D p = new Path2D.Float();
                p.moveTo(s * 0.2, s * 0.05);
                p.lineTo(s * 0.6, s * 0.05);
                p.lineTo(s * 0.85, s * 0.3);
                p.lineTo(s * 0.85, s * 0.95);
                p.lineTo(s * 0.2, s * 0.95);
                p.closePath();
                g.fill(p);
                g.setColor(new Color(255, 255, 255, 170));
                Path2D fold = new Path2D.Float();
                fold.moveTo(s * 0.6, s * 0.05);
                fold.lineTo(s * 0.6, s * 0.3);
                fold.lineTo(s * 0.85, s * 0.3);
                fold.closePath();
                g.fill(fold);
            }
        };
    }

    /** Palomitas de estado: una (enviado), dos (entregado) o dos azules (leído). */
    public static Icon ticks(int count, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < count; i++) {
                    int ox = x + i * 5;
                    Path2D p = new Path2D.Float();
                    p.moveTo(ox + 1, y + 6);
                    p.lineTo(ox + 4, y + 9);
                    p.lineTo(ox + 10, y + 2);
                    g.draw(p);
                }
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 11;
            }
        };
    }

    /** Logo de Nexo: dos burbujas entrelazadas. */
    public static Icon logo(int size) {
        return new Painted(size, Theme.PRIMARY) {
            @Override
            void paint(Graphics2D g, float s) {
                g.fill(new Ellipse2D.Float(0, 0, s, s));
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(s * 0.2f, s * 0.24f, s * 0.42f, s * 0.32f, s * 0.16f, s * 0.16f));
                Path2D t1 = new Path2D.Float();
                t1.moveTo(s * 0.28, s * 0.54);
                t1.lineTo(s * 0.26, s * 0.66);
                t1.lineTo(s * 0.4, s * 0.54);
                g.fill(t1);
                g.setColor(new Color(0xC7D2FE));
                g.fill(new RoundRectangle2D.Float(s * 0.4f, s * 0.44f, s * 0.42f, s * 0.32f, s * 0.16f, s * 0.16f));
                Path2D t2 = new Path2D.Float();
                t2.moveTo(s * 0.74, s * 0.74);
                t2.lineTo(s * 0.76, s * 0.86);
                t2.lineTo(s * 0.62, s * 0.74);
                g.fill(t2);
            }
        };
    }
}
