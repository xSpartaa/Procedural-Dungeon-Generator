package dungeon;

import java.awt.Point;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Salle du donjon avec un ID unique.
 */
public class Room {
    public final int id;
    public int x, y, width, height;
    public RoomType type = RoomType.NORMAL;

    private static int nextId = 0;

    public Room(int x, int y, int w, int h) {
        this.id     = nextId++;
        this.x      = x;
        this.y      = y;
        this.width  = w;
        this.height = h;
    }

    public Point center() {
        return new Point(x + width / 2, y + height / 2);
    }

    /** Réinitialise le compteur d'IDs (à appeler avant chaque génération). */
    public static void resetIds() { nextId = 0; }

    static int clamp(int v, int lo, int hi) {
        if (lo > hi) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}