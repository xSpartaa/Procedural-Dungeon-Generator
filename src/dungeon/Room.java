package dungeon;

import java.util.concurrent.ThreadLocalRandom;
import java.awt.*;

public class Room {
    static final int MARGIN = (Corridor.THICKNESS / 2) + 3;

    public int x, y, width, height;
    public RoomType type = RoomType.NORMAL;

    public Room(Rectangle rect, boolean isBoss) {
        int maxW = rect.width  - MARGIN * 2;
        int maxH = rect.height - MARGIN * 2;
        int minSide = 10;
        maxW = Math.max(minSide, maxW);
        maxH = Math.max(minSide, maxH);

        double ratio = isBoss ? randRange(0.78, 0.92) : randRange(0.65, 0.88);
        this.width  = clamp((int)(maxW * ratio), minSide, maxW);
        this.height = clamp((int)(maxH * ratio), minSide, maxH);

        int spaceX = maxW - this.width;
        int spaceY = maxH - this.height;
        this.x = rect.x + MARGIN + (spaceX > 0 ? ThreadLocalRandom.current().nextInt(spaceX + 1) : 0);
        this.y = rect.y + MARGIN + (spaceY > 0 ? ThreadLocalRandom.current().nextInt(spaceY + 1) : 0);
        this.x = clamp(this.x, rect.x + MARGIN, rect.x + rect.width  - this.width  - MARGIN);
        this.y = clamp(this.y, rect.y + MARGIN, rect.y + rect.height - this.height - MARGIN);
    }

    public Room(Rectangle rect) { this(rect, false); }

    public Point center() { return new Point(x + width / 2, y + height / 2); }

    private static double randRange(double a, double b) {
        return a + ThreadLocalRandom.current().nextDouble() * (b - a);
    }
    static int clamp(int v, int lo, int hi) {
        if (lo > hi) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}