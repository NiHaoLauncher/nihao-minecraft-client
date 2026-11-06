package com.nihao.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Start-up window to choose how to sign in. Swing, because it runs before Minecraft (and its GLFW window) exists. It must not touch
 * Minecraft classes: it runs in a JVM that only has the mod jar, and reads its texts from the mod's own lang files.
 */
final class LoginWindow {
    record Result(String type, ElyAuth.Profile profile, boolean remember) {}

    /** The window was closed without a choice: the game must not start. */
    static final Result CLOSED = new Result("closed", null, false);

    // the colours of the NiHao launcher
    private static final Color BG = new Color(0x191919), CARD = new Color(0x202020), CARD_HI = new Color(0x262B33), LINE = new Color(0x2E2E2E),
            MUTED = new Color(0x9A9AA0), ACCENT = new Color(0x4A90D9), ACCENT_HI = new Color(0x69ACF5), DANGER = new Color(0xFF6B6B);
    private static final int W = 340; // width of the content

    private static Result result;
    private static Map<String, String> texts = Map.of(), english = Map.of();

    private LoginWindow() {}

    /**
     * Blocks until the user chooses: {@link #CLOSED} if the window was closed, null if it could not be shown. {@code licensedName} is null
     * when the game was not started from a licensed account. The window runs in a separate JVM: by the time the game's Main runs,
     * java.awt.headless is already true there (and on macOS AWT cannot start next to GLFW).
     */
    static Result show(String nick, String licensedName, java.nio.file.Path classpath, boolean rememberDefault, String lang) throws Exception {
        String javaBin = java.nio.file.Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        Process proc = new ProcessBuilder(javaBin, "-Djava.awt.headless=false", "-cp", classpath.toString(), LoginWindow.class.getName(),
                enc(nick), licensedName == null ? "-" : enc(licensedName), rememberDefault ? "1" : "0", lang).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        Result result = null;
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (line.equals("CLOSED")) result = CLOSED;
                if (!line.startsWith("RESULT\t")) continue;
                String[] f = line.split("\t", -1);
                result = new Result(dec(f[1]), f[2].isEmpty() ? null : new ElyAuth.Profile(dec(f[2]), dec(f[3]), dec(f[4]), dec(f[5])), f[6].equals("1"));
            }
        }
        proc.waitFor();
        return result;
    }

    /** The lang file for the window: Minecraft's own language (options.txt) when set, else the system language. */
    static String language(java.nio.file.Path gameDir) {
        String code = java.util.Locale.getDefault().getLanguage();
        try {
            for (String line : java.nio.file.Files.readAllLines(gameDir.resolve("options.txt"))) if (line.startsWith("lang:")) code = line.substring(5).trim();
        } catch (Exception ignored) {
            // no options yet: the system language
        }
        code = code.toLowerCase();
        return code.startsWith("uk") ? "uk_ua" : code.startsWith("ru") ? "ru_ru" : "en_us";
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String dec(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    /** Entry point of the child JVM: shows the window and prints one RESULT line, or CLOSED when it was closed without a choice. */
    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        init(args[3]);
        showHere(dec(args[0]), args[1].equals("-") ? null : dec(args[1]), !args[2].equals("0"));
        Result r = result;
        if (r != null) {
            ElyAuth.Profile p = r.profile();
            System.out.println("RESULT\t" + enc(r.type()) + "\t" + (p == null ? "\t\t\t" : enc(p.name()) + "\t" + enc(p.uuid()) + "\t" + enc(p.accessToken()) + "\t" + enc(p.clientToken()))
                    + "\t" + (r.remember() ? "1" : "0"));
        } else {
            System.out.println("CLOSED");
        }
        System.out.flush();
        System.exit(0);
    }

    static void init(String lang) {
        english = load("en_us");
        texts = load(lang);
    }

    /** The flat "key": "value" pairs of a lang file (no Gson here: the child JVM only has the mod jar). */
    private static Map<String, String> load(String lang) {
        Map<String, String> m = new HashMap<>();
        try (var in = LoginWindow.class.getResourceAsStream("/assets/nihao/lang/" + lang + ".json")) {
            if (in == null) return m;
            var mt = Pattern.compile("\"([^\"\\\\]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            while (mt.find()) m.put(mt.group(1), mt.group(2).replace("\\\"", "\"").replace("\\\\", "\\"));
        } catch (Exception ignored) {
            // the keys themselves are shown
        }
        return m;
    }

    private static String tr(String key) {
        return texts.getOrDefault("nihao." + key, english.getOrDefault("nihao." + key, key));
    }

    private static void showHere(String nick, String licensedName, boolean rememberDefault) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JDialog d = build(nick, licensedName, rememberDefault);
            d.setLocationRelativeTo(null);
            d.setAlwaysOnTop(true);
            d.setVisible(true); // modal: returns once disposed
        });
    }

    /** The window, packed but not shown (a test renders it to a picture). */
    static JDialog build(String nick, String licensedName, boolean rememberDefault) {
        JDialog d = new JDialog((Frame) null, "NiHaoClient", true);
        d.setUndecorated(true); // own title bar in the colours of the launcher
        d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                d.dispose(); // no result: the game does not start
            }
        });
        java.awt.Image logo = null;
        try (var in = LoginWindow.class.getResourceAsStream("/icon.png")) {
            if (in != null) logo = ImageIO.read(in);
        } catch (Exception ignored) {
            // no icon
        }
        if (logo != null) d.setIconImage(logo);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new LineBorder(LINE));
        JPanel bar = titleBar(d);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 20, 20, 20));

        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(W, 44));
        if (logo != null) head.add(new JLabel(new ImageIcon(logo.getScaledInstance(32, 32, Image.SCALE_REPLICATE))), BorderLayout.WEST);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(label("NiHaoClient", Font.BOLD, 20, Color.WHITE));
        titles.add(label(tr("login.prompt"), Font.PLAIN, 13, MUTED));
        head.add(titles, BorderLayout.CENTER);
        body.add(head);
        body.add(Box.createVerticalStrut(16));

        JCheckBox remember = new JCheckBox(tr("login.remember"), rememberDefault);
        remember.setOpaque(false);
        remember.setForeground(Color.WHITE);
        remember.setFont(font(Font.PLAIN, 13));
        remember.setFocusPainted(false);
        remember.setIcon(CHECK);
        remember.setAlignmentX(Component.LEFT_ALIGNMENT);
        remember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (licensedName != null) {
            body.add(new Card(tr("login.microsoft"), licensedName, false, () -> done(d, new Result("microsoft", null, remember.isSelected()))));
            body.add(Box.createVerticalStrut(8));
        }
        body.add(new Card(tr("login.offline"), nick, false, () -> done(d, new Result("offline", null, remember.isSelected()))));
        body.add(Box.createVerticalStrut(8));

        // Ely.by: the card opens the log-in form under it
        JPanel ely = new JPanel();
        ely.setOpaque(false);
        ely.setAlignmentX(Component.LEFT_ALIGNMENT);
        ely.setLayout(new BoxLayout(ely, BoxLayout.Y_AXIS));
        JTextField user = field(new JTextField());
        JPasswordField pass = field(new JPasswordField());
        JLabel status = label(" ", Font.PLAIN, 12, MUTED);
        Card go = new Card(tr("login.submit"), null, true, null);
        ely.add(Box.createVerticalStrut(4));
        ely.add(label(tr("login.elyby_user"), Font.PLAIN, 12, MUTED));
        ely.add(Box.createVerticalStrut(4));
        ely.add(user);
        ely.add(Box.createVerticalStrut(10));
        ely.add(label(tr("login.elyby_password"), Font.PLAIN, 12, MUTED));
        ely.add(Box.createVerticalStrut(4));
        ely.add(pass);
        ely.add(Box.createVerticalStrut(12));
        ely.add(go);
        ely.add(Box.createVerticalStrut(6));
        ely.add(status);
        ely.setVisible(false);
        Runnable login = () -> {
            go.setEnabled(false);
            status.setForeground(MUTED);
            status.setText(tr("login.progress"));
            new Thread(() -> {
                try {
                    ElyAuth.Profile pr = ElyAuth.login(user.getText().trim(), new String(pass.getPassword()), UUID.randomUUID().toString().replace("-", ""));
                    SwingUtilities.invokeLater(() -> done(d, new Result("elyby", pr, remember.isSelected())));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        status.setForeground(DANGER);
                        status.setText("<html><div style='width:" + (W - 10) + "px'>" + String.valueOf(ex.getMessage()).replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
                        go.setEnabled(true);
                        d.pack();
                    });
                }
            }).start();
        };
        go.onClick(login);
        user.addActionListener(e -> pass.requestFocusInWindow());
        pass.addActionListener(e -> login.run());
        body.add(new Card(tr("login.elyby"), tr("login.elyby_hint"), false, () -> {
            ely.setVisible(!ely.isVisible());
            d.pack();
            if (ely.isVisible()) user.requestFocusInWindow();
        }));
        body.add(ely);
        body.add(Box.createVerticalStrut(16));
        body.add(remember);

        root.add(bar, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        d.setContentPane(root);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        root.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        });
        d.pack();
        return d;
    }

    /** Text through TextLayout: the build renames the literal ".drawString(" to ".text(" on 26.x (Minecraft's GuiGraphics), which must not hit AWT. */
    private static void text(Graphics2D g, String s, int x, int y) {
        new java.awt.font.TextLayout(s, g.getFont(), g.getFontRenderContext()).draw(g, x, y);
    }

    private static Font font(int style, int size) {
        Font f = new Font("Segoe UI", style, size);
        return f.getFamily().equals("Segoe UI") ? f : new Font(Font.SANS_SERIF, style, size);
    }

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static <T extends JTextField> T field(T f) {
        f.setFont(font(Font.PLAIN, 13));
        f.setBackground(CARD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(W, 34));
        Runnable[] border = new Runnable[1];
        boolean[] focused = {false};
        border[0] = () -> f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(focused[0] ? ACCENT : LINE, 1, true), new EmptyBorder(6, 10, 6, 10)));
        border[0].run();
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused[0] = true;
                border[0].run();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused[0] = false;
                border[0].run();
            }
        });
        return f;
    }

    /** The window has no system title bar: a name, a close cross and a drag area. */
    private static JPanel titleBar(JDialog d) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 34));
        JLabel name = label("  NiHaoClient", Font.PLAIN, 12, MUTED);
        JButton close = new JButton() {
            private boolean hover;

            {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setPreferredSize(new Dimension(46, 34));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(hover ? DANGER : MUTED);
                g.setStroke(new BasicStroke(1.4f));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                g.drawLine(cx - 5, cy + 5, cx + 5, cy - 5);
                g.dispose();
            }
        };
        close.addActionListener(e -> d.dispose());
        bar.add(name, BorderLayout.CENTER);
        bar.add(close, BorderLayout.EAST);
        Point[] grab = new Point[1];
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                grab[0] = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                d.setLocation(e.getXOnScreen() - grab[0].x, e.getYOnScreen() - grab[0].y);
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        name.addMouseListener(drag);
        name.addMouseMotionListener(drag);
        return bar;
    }

    /** A custom check box: a rounded square, filled with the accent colour and a tick when selected. */
    private static final Icon CHECK = new Icon() {
        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean on = ((AbstractButton) c).isSelected();
            g.setColor(on ? ACCENT : CARD);
            g.fillRoundRect(x, y, 16, 16, 5, 5);
            g.setColor(on ? ACCENT : MUTED);
            g.drawRoundRect(x, y, 16, 16, 5, 5);
            if (on) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{x + 4, x + 7, x + 12}, new int[]{y + 8, y + 11, y + 5}, 3);
            }
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    };

    /** A flat rounded button: a dark card with a title and a muted second line, or (primary) a solid accent button. */
    private static final class Card extends JButton {
        private final String title, sub;
        private final boolean primary;
        private boolean hover;

        Card(String title, String sub, boolean primary, Runnable action) {
            this.title = title;
            this.sub = sub;
            this.primary = primary;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            Dimension size = new Dimension(W, primary ? 38 : 54);
            setPreferredSize(size);
            setMaximumSize(size);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            if (action != null) onClick(action);
        }

        void onClick(Runnable action) {
            addActionListener(e -> action.run());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (primary) {
                g.setColor(!isEnabled() ? ACCENT.darker().darker() : hover ? ACCENT_HI : ACCENT);
                g.fillRoundRect(0, 0, w, h, 10, 10);
                g.setFont(font(Font.BOLD, 13));
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                text(g, title, (w - fm.stringWidth(title)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            } else {
                g.setColor(hover ? CARD_HI : CARD);
                g.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g.setColor(hover ? ACCENT : LINE);
                g.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g.setFont(font(Font.BOLD, 14));
                g.setColor(Color.WHITE);
                text(g, title, 16, h / 2 - 4);
                if (sub != null) {
                    g.setFont(font(Font.PLAIN, 12));
                    g.setColor(MUTED);
                    text(g, sub, 16, h / 2 + 13);
                }
                g.setColor(hover ? ACCENT_HI : MUTED);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{w - 22, w - 16, w - 22}, new int[]{h / 2 - 5, h / 2, h / 2 + 5}, 3);
            }
            g.dispose();
        }
    }

    private static void done(JDialog d, Result r) {
        result = r;
        d.dispose();
    }
}
