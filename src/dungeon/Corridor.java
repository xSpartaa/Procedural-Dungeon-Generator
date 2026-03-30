package dungeon;

import java.awt.*;

/**
 * Couloir en L reliant le centre de deux salles feuilles adjacentes dans l'arbre BSP.
 * Un couloir se compose de deux segments : horizontal puis vertical (ou l'inverse).
 */
public class Corridor {
    public final Point from;
    public final Point to;
    public final int width = 2; // épaisseur du couloir en unités logiques

    public Corridor(Point from, Point to) {
        this.from = from;
        this.to   = to;
    }

    /**
     * Dessine le couloir en L (horizontal + vertical) sur le Graphics fourni.
     * Les coordonnées logiques sont multipliées par scale avant le dessin.
     */
    public void draw(Graphics2D g2, int scale) {
        int x1 = from.x * scale;
        int y1 = from.y * scale;
        int x2 = to.x   * scale;
        int y2 = to.y   * scale;
        int w  = width  * scale;

        // Segment horizontal (de x1,y1 vers x2,y1)
        int hx = Math.min(x1, x2);
        int hLen = Math.abs(x2 - x1) + w;
        g2.fillRect(hx, y1 - w / 2, hLen, w);

        // Segment vertical (de x2,y1 vers x2,y2)
        int vy = Math.min(y1, y2);
        int vLen = Math.abs(y2 - y1) + w;
        g2.fillRect(x2 - w / 2, vy, w, vLen);
    }
}