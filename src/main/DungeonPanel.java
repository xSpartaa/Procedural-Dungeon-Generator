package main;

import dungeon.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel de rendu du donjon.
 *
 * Contrôles :
 *  - [R] ou double-clic → régénère un nouveau donjon
 *  - molette / [+]/[-] → zoom
 *  - drag → pan
 */
public class DungeonPanel extends JPanel implements Runnable {

    // --- Rendu ---
    private static final int SCALE       = 7;
    private static final int WORLD_W     = 120;
    private static final int WORLD_H     = 110;

    // --- Couleurs ---
    private static final Color COL_BG        = new Color(10, 10, 18);
    private static final Color COL_CONTAINER = new Color(30, 35, 50);
    private static final Color COL_ROOM      = new Color(180, 40, 40);
    private static final Color COL_CORRIDOR  = new Color(120, 90, 60);
    private static final Color COL_ENTRANCE  = new Color(60, 200, 80);
    private static final Color COL_MINI_BOSS = new Color(220, 130, 20);
    private static final Color COL_BOSS      = new Color(140, 50, 200);
    private static final Color COL_LABEL_FG  = new Color(240, 240, 240);
    private static final Color COL_GRID      = new Color(25, 30, 45);

    // --- État ---
    private DungeonGenerator dungeon;

    // --- Pan / Zoom ---
    private double zoom     = 1.0;
    private int    panX     = 20;
    private int    panY     = 20;
    private int    dragStartX, dragStartY, panStartX, panStartY;

    // ---------------------------------------------------------------

    public DungeonPanel() {
        setBackground(COL_BG);
        setFocusable(true);
        regenerate();
        setupControls();
        new Thread(this).start();
    }

    private void regenerate() {
        dungeon = new DungeonGenerator(new Rectangle(0, 0, WORLD_W, WORLD_H));
        repaint();
    }

    // ---------------------------------------------------------------
    // Rendu
    // ---------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dungeon == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Appliquer le zoom + pan
        g2.translate(panX, panY);
        g2.scale(zoom, zoom);

        int s = SCALE;

        // 1. Grille de fond
        drawGrid(g2, s);

        // 2. Conteneurs BSP (légers, pour débug visuel)
        drawContainers(g2, s);

        // 3. Couloirs (dessinés AVANT les salles pour être sous elles)
        drawCorridors(g2, s);

        // 4. Salles
        drawRooms(g2, s);

        // 5. Légende
        g2.setTransform(new java.awt.geom.AffineTransform()); // reset transform
        drawLegend(g2);
    }

    private void drawGrid(Graphics2D g2, int s) {
        g2.setColor(COL_GRID);
        g2.setStroke(new BasicStroke(0.5f));
        int totalW = WORLD_W * s;
        int totalH = WORLD_H * s;
        for (int x = 0; x <= WORLD_W; x += 5)
            g2.drawLine(x * s, 0, x * s, totalH);
        for (int y = 0; y <= WORLD_H; y += 5)
            g2.drawLine(0, y * s, totalW, y * s);
    }

    private void drawContainers(Graphics2D g2, int s) {
        g2.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{4, 4}, 0f));
        drawContainerNode(g2, dungeon.root, s, 0);
    }

    private void drawContainerNode(Graphics2D g2, Node node, int s, int depth) {
        if (node == null) return;
        // Couleur plus sombre pour les niveaux profonds
        int alpha = Math.max(20, 60 - depth * 8);
        g2.setColor(new Color(50, 60, 90, alpha));
        g2.drawRect(node.rect.x * s, node.rect.y * s, node.rect.width * s, node.rect.height * s);
        drawContainerNode(g2, node.left,  s, depth + 1);
        drawContainerNode(g2, node.right, s, depth + 1);
    }

    private void drawCorridors(Graphics2D g2, int s) {
        g2.setColor(COL_CORRIDOR);
        g2.setStroke(new BasicStroke(1f));
        List<Corridor> corridors = dungeon.corridors;
        for (Corridor c : corridors) {
            c.draw(g2, s);
        }
    }

    private void drawRooms(Graphics2D g2, int s) {
        for (Node leaf : dungeon.leaves) {
            Room room = leaf.room;
            if (room == null) continue;

            Color fill   = roomColor(room.type);
            Color border = fill.brighter();

            // Ombre douce
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(room.x * s + 3, room.y * s + 3,
                    room.width * s, room.height * s, 6, 6);

            // Corps de la salle
            g2.setColor(fill);
            g2.fillRoundRect(room.x * s, room.y * s,
                    room.width * s, room.height * s, 6, 6);

            // Bordure
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(room.x * s, room.y * s,
                    room.width * s, room.height * s, 6, 6);

            // Label pour les salles spéciales
            String label = roomLabel(room.type);
            if (!label.isEmpty()) {
                g2.setColor(COL_LABEL_FG);
                g2.setFont(new Font("Monospaced", Font.BOLD, (int)(9 * zoom + 1)));
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(label);
                int cx = room.x * s + (room.width  * s - lw) / 2;
                int cy = room.y * s + (room.height * s + fm.getAscent()) / 2 - 2;
                // Fond du label
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRoundRect(cx - 3, cy - fm.getAscent(), lw + 6, fm.getHeight() + 2, 4, 4);
                g2.setColor(COL_LABEL_FG);
                g2.drawString(label, cx, cy);
            }
        }
    }

    private void drawLegend(Graphics2D g2) {
        int x = 12, y = getHeight() - 110;
        int bh = 18, bw = 18, gap = 6, lineH = 26;

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        String[][] entries = {
                {"Entrée",    toHex(COL_ENTRANCE)},
                {"Mini-boss", toHex(COL_MINI_BOSS)},
                {"Boss",      toHex(COL_BOSS)},
                {"Salle",     toHex(COL_ROOM)},
                {"Couloir",   toHex(COL_CORRIDOR)},
        };

        g2.setColor(new Color(10, 12, 20, 200));
        g2.fillRoundRect(x - 6, y - 20, 140, entries.length * lineH + 28, 10, 10);
        g2.setColor(new Color(60, 70, 100));
        g2.drawRoundRect(x - 6, y - 20, 140, entries.length * lineH + 28, 10, 10);

        g2.setColor(new Color(180, 180, 200));
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.drawString("Légende  [R]=regen", x, y - 4);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));

        for (int i = 0; i < entries.length; i++) {
            Color c = Color.decode(entries[i][1]);
            g2.setColor(c);
            g2.fillRoundRect(x, y + i * lineH, bw, bh, 4, 4);
            g2.setColor(c.brighter());
            g2.drawRoundRect(x, y + i * lineH, bw, bh, 4, 4);
            g2.setColor(new Color(200, 200, 220));
            g2.drawString(entries[i][0], x + bw + gap, y + i * lineH + bh - 3);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Color roomColor(RoomType type) {
        return switch (type) {
            case ENTRANCE  -> COL_ENTRANCE;
            case MINI_BOSS -> COL_MINI_BOSS;
            case BOSS      -> COL_BOSS;
            default        -> COL_ROOM;
        };
    }

    private static String roomLabel(RoomType type) {
        return switch (type) {
            case ENTRANCE  -> "ENTRÉE";
            case MINI_BOSS -> "MINI-BOSS";
            case BOSS      -> "BOSS";
            default        -> "";
        };
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---------------------------------------------------------------
    // Contrôles (zoom, pan, regen)
    // ---------------------------------------------------------------

    private void setupControls() {
        // Zoom molette
        addMouseWheelListener(e -> {
            double factor = (e.getWheelRotation() < 0) ? 1.1 : 0.9;
            zoom = Math.max(0.3, Math.min(5.0, zoom * factor));
            repaint();
        });

        // Pan (drag)
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragStartX = e.getX(); dragStartY = e.getY();
                panStartX  = panX;     panStartY  = panY;
                requestFocusInWindow();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) regenerate();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                panX = panStartX + (e.getX() - dragStartX);
                panY = panStartY + (e.getY() - dragStartY);
                repaint();
            }
        });

        // Clavier
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_R -> regenerate();
                    case KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS ->
                    { zoom = Math.min(5.0, zoom * 1.15); repaint(); }
                    case KeyEvent.VK_MINUS ->
                    { zoom = Math.max(0.3, zoom / 1.15); repaint(); }
                }
            }
        });
    }

    // ---------------------------------------------------------------
    // Boucle de rendu (~60 FPS)
    // ---------------------------------------------------------------

    @Override
    public void run() {
        while (true) {
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }
}