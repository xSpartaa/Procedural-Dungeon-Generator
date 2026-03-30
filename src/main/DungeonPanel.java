package main;

import dungeon.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel de rendu.
 * Contrôles : [R] / double-clic = régénère | molette = zoom | drag = pan
 */
public class DungeonPanel extends JPanel implements Runnable {

    private static final int SCALE   = 7;
    private static final int WORLD_W = 120;
    private static final int WORLD_H = 110;

    // Palette
    private static final Color BG           = new Color(10, 10, 18);
    private static final Color COL_GRID     = new Color(22, 26, 40);
    private static final Color COL_CONT     = new Color(35, 42, 65, 60);
    private static final Color COL_CORRIDOR = new Color(100, 75, 45);
    private static final Color COL_COR_EDGE = new Color(140, 110, 70);
    private static final Color COL_ROOM     = new Color(165, 35, 35);
    private static final Color COL_ROOM_E   = new Color(220, 55, 55);
    private static final Color COL_ENTRANCE = new Color(50, 190, 70);
    private static final Color COL_ENTRANCE_E = new Color(80, 240, 100);
    private static final Color COL_MINIBOSS = new Color(210, 120, 15);
    private static final Color COL_MINIBOSS_E = new Color(255, 170, 40);
    private static final Color COL_BOSS     = new Color(120, 40, 190);
    private static final Color COL_BOSS_E   = new Color(170, 80, 240);
    private static final Color COL_LABEL    = new Color(235, 235, 245);

    private DungeonGenerator dungeon;
    private double zoom = 1.0;
    private int panX = 30, panY = 30;
    private int dragStartX, dragStartY, panStartX, panStartY;

    public DungeonPanel() {
        setBackground(BG);
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

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.translate(panX, panY);
        g2.scale(zoom, zoom);

        drawGrid(g2);
        drawContainers(g2, dungeon.root, 0);
        drawCorridors(g2);
        drawRooms(g2);

        g2.dispose();
        drawLegend((Graphics2D) g);
        drawHint((Graphics2D) g);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(COL_GRID);
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x <= WORLD_W; x += 5)
            g2.drawLine(x*SCALE, 0, x*SCALE, WORLD_H*SCALE);
        for (int y = 0; y <= WORLD_H; y += 5)
            g2.drawLine(0, y*SCALE, WORLD_W*SCALE, y*SCALE);
    }

    private void drawContainers(Graphics2D g2, Node node, int depth) {
        if (node == null) return;
        int alpha = Math.max(15, 55 - depth * 9);
        g2.setColor(new Color(50, 65, 110, alpha));
        g2.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{3, 5}, 0f));
        g2.drawRect(node.rect.x*SCALE, node.rect.y*SCALE,
                node.rect.width*SCALE, node.rect.height*SCALE);
        drawContainers(g2, node.left,  depth+1);
        drawContainers(g2, node.right, depth+1);
    }

    private void drawCorridors(Graphics2D g2) {
        for (Corridor c : dungeon.corridors) {
            // Contour sombre
            g2.setColor(new Color(40, 28, 15));
            drawCorridorFilled(g2, c, SCALE, Corridor.THICKNESS + 1);
            // Corps
            g2.setColor(COL_CORRIDOR);
            drawCorridorFilled(g2, c, SCALE, Corridor.THICKNESS);
            // Reflet léger (ligne fine au centre)
            g2.setColor(COL_COR_EDGE);
            g2.setStroke(new BasicStroke(0.8f));
            drawCorridorLine(g2, c, SCALE);
        }
    }

    /** Remplit le couloir en L avec une épaisseur donnée (en unités logiques). */
    private void drawCorridorFilled(Graphics2D g2, Corridor c, int scale, int thickness) {
        int x1 = c.from().x*scale, y1 = c.from().y*scale;
        int x2 = c.to().x*scale,   y2 = c.to().y*scale;
        int t  = thickness*scale;

        int midX = c.hFirst() ? x2 : x1;
        int midY = c.hFirst() ? y1 : y2;

        g2.fillRect(Math.min(x1,midX), y1-t/2, Math.abs(midX-x1)+t, t);
        g2.fillRect(midX-t/2, Math.min(y1,y2), t, Math.abs(y2-y1)+t);
    }

    /** Trace une ligne fine au centre du couloir (reflet). */
    private void drawCorridorLine(Graphics2D g2, Corridor c, int scale) {
        int x1 = c.from().x*scale, y1 = c.from().y*scale;
        int x2 = c.to().x*scale,   y2 = c.to().y*scale;
        int midX = c.hFirst() ? x2 : x1;
        int midY = c.hFirst() ? y1 : y2;
        g2.drawLine(x1, y1, midX, midY);
        g2.drawLine(midX, midY, x2, y2);
    }

    private void drawRooms(Graphics2D g2) {
        for (Node leaf : dungeon.leaves) {
            Room room = leaf.room;
            if (room == null) continue;

            Color[] cols = roomColors(room.type);
            Color fill = cols[0], edge = cols[1];

            int rx = room.x*SCALE, ry = room.y*SCALE;
            int rw = room.width*SCALE, rh = room.height*SCALE;

            // Ombre portée
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(rx+4, ry+4, rw, rh, 8, 8);

            // Sol (texture légère : dégradé simulé avec deux rect)
            g2.setColor(fill.darker());
            g2.fillRoundRect(rx, ry, rw, rh, 8, 8);
            g2.setColor(fill);
            g2.fillRoundRect(rx+1, ry+1, rw-2, rh/2, 8, 8);

            // Bordure
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(rx, ry, rw, rh, 8, 8);

            // Label salles spéciales
            String label = label(room.type);
            if (!label.isEmpty()) {
                Font f = new Font("Monospaced", Font.BOLD, 10);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(label);
                int cx = rx + (rw - lw) / 2;
                int cy = ry + (rh + fm.getAscent() - fm.getDescent()) / 2;
                // Fond du label
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(cx-4, cy-fm.getAscent()-1, lw+8, fm.getHeight()+2, 5, 5);
                g2.setColor(COL_LABEL);
                g2.drawString(label, cx, cy);
            }
        }
    }

    private void drawLegend(Graphics2D g2) {
        int x = 12, y = getHeight() - 140;
        Object[][] entries = {
                {COL_ENTRANCE, "Entrée"},
                {COL_MINIBOSS, "Mini-boss"},
                {COL_BOSS,     "Boss"},
                {COL_ROOM,     "Salle"},
                {COL_CORRIDOR, "Couloir"},
        };
        int lineH = 22, bw = 16, bh = 16, pad = 8;
        int boxW = 148, boxH = entries.length * lineH + 34;

        g2.setColor(new Color(8, 10, 20, 210));
        g2.fillRoundRect(x-6, y-24, boxW, boxH, 10, 10);
        g2.setColor(new Color(55, 70, 110));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x-6, y-24, boxW, boxH, 10, 10);

        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(new Color(170, 180, 210));
        g2.drawString("Légende   [R]=regen", x, y-6);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        for (int i = 0; i < entries.length; i++) {
            Color c = (Color) entries[i][0];
            g2.setColor(c);
            g2.fillRoundRect(x, y + i*lineH, bw, bh, 4, 4);
            g2.setColor(c.brighter());
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y + i*lineH, bw, bh, 4, 4);
            g2.setColor(new Color(200, 205, 225));
            g2.drawString((String) entries[i][1], x + bw + pad, y + i*lineH + bh - 2);
        }
    }

    private void drawHint(Graphics2D g2) {
        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.setColor(new Color(80, 90, 120));
        g2.drawString("Molette=zoom  Drag=pan  DblClic=regen", 12, getHeight()-8);
    }

    // ---------------------------------------------------------------
    // Helpers couleur / label
    // ---------------------------------------------------------------

    private static Color[] roomColors(RoomType t) {
        return switch (t) {
            case ENTRANCE  -> new Color[]{COL_ENTRANCE,  COL_ENTRANCE_E};
            case MINI_BOSS -> new Color[]{COL_MINIBOSS,  COL_MINIBOSS_E};
            case BOSS      -> new Color[]{COL_BOSS,       COL_BOSS_E};
            default        -> new Color[]{COL_ROOM,       COL_ROOM_E};
        };
    }

    private static String label(RoomType t) {
        return switch (t) {
            case ENTRANCE  -> "ENTRÉE";
            case MINI_BOSS -> "MINI-BOSS";
            case BOSS      -> "★ BOSS ★";
            default        -> "";
        };
    }

    // ---------------------------------------------------------------
    // Contrôles
    // ---------------------------------------------------------------

    private void setupControls() {
        addMouseWheelListener(e -> {
            double f = e.getWheelRotation() < 0 ? 1.12 : 0.89;
            zoom = Math.max(0.25, Math.min(6.0, zoom * f));
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStartX = e.getX(); dragStartY = e.getY();
                panStartX  = panX;    panStartY  = panY;
                requestFocusInWindow();
            }
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) regenerate();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                panX = panStartX + (e.getX() - dragStartX);
                panY = panStartY + (e.getY() - dragStartY);
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) regenerate();
            }
        });
    }

    @Override
    public void run() {
        while (true) {
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }
}