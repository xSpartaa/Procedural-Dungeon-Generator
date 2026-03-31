package dungeon;

import java.awt.Point;

/**
 * Couloir en L entre deux salles.
 * doorA = centre du bord de roomA côté couloir
 * doorB = centre du bord de roomB côté couloir
 * midX/midY = coude du L
 */
public class Corridor {

    public static final int THICKNESS = 3;

    public final Room  roomA, roomB;
    public final Point doorA, doorB;
    public final int   midX, midY;
    public final Side  sideA, sideB;

    public enum Side { TOP, BOTTOM, LEFT, RIGHT }

    public Corridor(Room a, Room b) {
        this.roomA = a;
        this.roomB = b;

        Point ca = a.center();
        Point cb = b.center();
        int dx = cb.x - ca.x;
        int dy = cb.y - ca.y;

        boolean hFirst = Math.abs(dx) >= Math.abs(dy);

        if (hFirst) {
            // Seg1 horizontal, Seg2 vertical
            midX = cb.x;
            midY = ca.y;

            if (dx >= 0) {
                doorA = new Point(a.x + a.width, ca.y); sideA = Side.RIGHT;
            } else {
                doorA = new Point(a.x, ca.y);            sideA = Side.LEFT;
            }

            if (dy == 0) {
                if (dx >= 0) { doorB = new Point(b.x, cb.y);           sideB = Side.LEFT; }
                else         { doorB = new Point(b.x + b.width, cb.y); sideB = Side.RIGHT; }
            } else if (dy > 0) {
                doorB = new Point(cb.x, b.y);            sideB = Side.TOP;
            } else {
                doorB = new Point(cb.x, b.y + b.height); sideB = Side.BOTTOM;
            }

        } else {
            // Seg1 vertical, Seg2 horizontal
            midX = ca.x;
            midY = cb.y;

            if (dy >= 0) {
                doorA = new Point(ca.x, a.y + a.height); sideA = Side.BOTTOM;
            } else {
                doorA = new Point(ca.x, a.y);             sideA = Side.TOP;
            }

            if (dx == 0) {
                if (dy >= 0) { doorB = new Point(cb.x, b.y);            sideB = Side.TOP; }
                else         { doorB = new Point(cb.x, b.y + b.height); sideB = Side.BOTTOM; }
            } else if (dx > 0) {
                doorB = new Point(b.x, cb.y);             sideB = Side.LEFT;
            } else {
                doorB = new Point(b.x + b.width, cb.y);  sideB = Side.RIGHT;
            }
        }
    }
}