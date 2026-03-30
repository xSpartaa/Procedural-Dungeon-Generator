package main;

import dungeon.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class DungeonPanel extends JPanel implements Runnable {

    private static final int SCALE   = 7;
    private static final int WORLD_W = 120;
    private static final int WORLD_H = 110;

    // Palette
    private static final Color BG             = new Color(10, 10, 18);
    private static final Color COL_GRID       = new Color(20, 24, 38);
    private static final Color COL_CORRIDOR   = new Color(95, 72, 42);
    private static final Color COL_COR_DARK   = new Color(38, 28, 14);
    private static final Color COL_COR_LIGHT  = new Color(145, 115, 72);
    private static final Color COL_ROOM_FILL  = new Color(160, 32, 32);
    private static final Color COL_ROOM_EDGE  = new Color(215, 55, 55);
    private static final Color COL_ENTRANCE_F = new Color(45, 185, 65);
    private static final Color COL_ENTRANCE_E = new Color(75, 235, 95);
    private static final Color COL_MBOSS_F    = new Color(205, 115, 10);
    private static final Color COL_MBOSS_E    = new Color(255, 165, 35);
    private static final Color COL_BOSS_F     = new Color(115, 35, 185);
    private static final Color COL_BOSS_E     = new Color(165, 75, 235);
    private static final Color COL_LABEL      = new Color(235, 235, 245);

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
    }

    // --- Grille ---
    private void drawGrid(Graphics2D g2) {
        g2.setColor(COL_GRID);
        g2.setStroke(new BasicStroke(0.4f));
        for (int x = 0; x <= WORLD_W; x += 5)
            g2.drawLine(x*SCALE, 0, x*SCALE, WORLD_H*SCALE);
        for (int y = 0; y <= WORLD_H; y += 5)
            g2.drawLine(0, y*SCALE, WORLD_W*SCALE, y*SCALE);
    }

    // --- Conteneurs BSP (debug léger) ---
    private void drawContainers(Graphics2D g2, Node node, int depth) {
        if (node == null) return;
        g2.setColor(new Color(45, 60, 100, Math.max(10, 50 - depth*9)));
        g2.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{3, 6}, 0f));
        g2.drawRect(node.rect.x*SCALE, node.rect.y*SCALE,
                node.rect.width*SCALE, node.rect.height*SCALE);
        drawContainers(g2, node.left,  depth+1);
        drawContainers(g2, node.right, depth+1);
    }

    // --- Couloirs ---
    private void drawCorridors(Graphics2D g2) {
        for (Corridor c : dungeon.corridors) {
            // Ombre extérieure
            g2.setColor(COL_COR_DARK);
            c.draw(g2, SCALE, Corridor.THICKNESS + 1);
            // Corps
            g2.setColor(COL_CORRIDOR);
            c.draw(g2, SCALE, Corridor.THICKNESS);
            // Reflet central
            g2.setColor(COL_COR_LIGHT);
            g2.setStroke(new BasicStroke(0.7f));
            int px1=c.x1*SCALE, py1=c.y1*SCALE, pmx=c.midX*SCALE, pmy=c.midY*SCALE;
            int px2=c.x2*SCALE, py2=c.y2*SCALE;
            g2.drawLine(px1, py1, pmx, pmy);
            g2.drawLine(pmx, pmy, px2, py2);
        }
    }

    // --- Salles ---
    private void drawRooms(Graphics2D g2) {
        for (Node leaf : dungeon.leaves) {
            Room room = leaf.room;
            if (room == null) continue;

            Color[] cols = roomColors(room.type);
            int rx = room.x*SCALE, ry = room.y*SCALE;
            int rw = room.width*SCALE, rh = room.height*SCALE;

            // Ombre portée
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(rx+4, ry+5, rw, rh, 8, 8);

            // Sol sombre
            g2.setColor(cols[0].darker());
            g2.fillRoundRect(rx, ry, rw, rh, 8, 8);
            // Partie haute plus claire (lumière venue du plafond)
            g2.setColor(cols[0]);
            g2.fillRoundRect(rx+1, ry+1, rw-2, rh*2/3, 7, 7);

            // Bordure lumineuse
            g2.setColor(cols[1]);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(rx, ry, rw, rh, 8, 8);

            // Label
            String lbl = label(room.type);
            if (!lbl.isEmpty()) {
                g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(lbl);
                int cx = rx + (rw - lw) / 2;
                int cy = ry + (rh + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRoundRect(cx-4, cy-fm.getAscent()-1, lw+8, fm.getHeight()+2, 4, 4);
                g2.setColor(COL_LABEL);
                g2.drawString(lbl, cx, cy);
            }
        }
    }

    // --- Légende ---
    private void drawLegend(Graphics2D g2) {
        Object[][] e = {
                {COL_ENTRANCE_F, "Entrée"},
                {COL_MBOSS_F,    "Mini-boss"},
                {COL_BOSS_F,     "Boss"},
                {COL_ROOM_FILL,  "Salle normale"},
                {COL_CORRIDOR,   "Couloir"},
        };
        int x=12, y=getHeight()-148, lh=22, bw=16, bh=16, pd=8;
        g2.setColor(new Color(8,10,20,215));
        g2.fillRoundRect(x-6, y-24, 158, e.length*lh+34, 10, 10);
        g2.setColor(new Color(50,65,105));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x-6, y-24, 158, e.length*lh+34, 10, 10);
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(new Color(165,175,210));
        g2.drawString("Légende   [R]=regen", x, y-6);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        for (int i = 0; i < e.length; i++) {
            Color c = (Color)e[i][0];
            g2.setColor(c); g2.fillRoundRect(x, y+i*lh, bw, bh, 4, 4);
            g2.setColor(c.brighter()); g2.drawRoundRect(x, y+i*lh, bw, bh, 4, 4);
            g2.setColor(new Color(195,200,225));
            g2.drawString((String)e[i][1], x+bw+pd, y+i*lh+bh-2);
        }
        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.setColor(new Color(70,80,115));
        g2.drawString("Molette=zoom  Drag=pan", 12, getHeight()-6);
    }

    private static Color[] roomColors(RoomType t) {
        return switch (t) {
            case ENTRANCE  -> new Color[]{COL_ENTRANCE_F, COL_ENTRANCE_E};
            case MINI_BOSS -> new Color[]{COL_MBOSS_F,    COL_MBOSS_E};
            case BOSS      -> new Color[]{COL_BOSS_F,     COL_BOSS_E};
            default        -> new Color[]{COL_ROOM_FILL,  COL_ROOM_EDGE};
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

    private void setupControls() {
        addMouseWheelListener(e -> {
            double f = e.getWheelRotation() < 0 ? 1.12 : 0.89;
            zoom = Math.max(0.2, Math.min(6.0, zoom * f));
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStartX=e.getX(); dragStartY=e.getY();
                panStartX=panX; panStartY=panY;
                requestFocusInWindow();
            }
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) regenerate();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                panX=panStartX+(e.getX()-dragStartX);
                panY=panStartY+(e.getY()-dragStartY);
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_R) regenerate();
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