package dungeon;

import java.util.concurrent.ThreadLocalRandom;
import java.awt.*;

public class Room {
    static final int MIN_ROOM_SIZE = 8;
    static final int MARGIN = 2;
    public int x, y, width, height;
    public RoomType type = RoomType.NORMAL;


    public Room(Rectangle rect) {
        this(rect, false);
    }


    public Room(Rectangle rect, boolean isBoss) {
        // Taille max disponible en tenant compte des marges
        int maxW = rect.width  - MARGIN * 2;
        int maxH = rect.height - MARGIN * 2;

        if (maxW < MIN_ROOM_SIZE) maxW = Math.max(1, rect.width - 1);
        if (maxH < MIN_ROOM_SIZE) maxH = Math.max(1, rect.height - 1);

        int minW = Math.min(MIN_ROOM_SIZE, maxW);
        int minH = Math.min(MIN_ROOM_SIZE, maxH);

        if (isBoss) {
            this.width  = clamp((int)(maxW * randRange(0.65, 0.85)), minW, maxW);
            this.height = clamp((int)(maxH * randRange(0.65, 0.85)), minH, maxH);
        } else {
            this.width  = clamp((int)(maxW * randRange(0.50, 0.80)), minW, maxW);
            this.height = clamp((int)(maxH * randRange(0.50, 0.80)), minH, maxH);
        }

        int spaceX = maxW - this.width;
        int spaceY = maxH - this.height;

        this.x = rect.x + MARGIN + (spaceX > 0 ? ThreadLocalRandom.current().nextInt(spaceX + 1) : 0);
        this.y = rect.y + MARGIN + (spaceY > 0 ? ThreadLocalRandom.current().nextInt(spaceY + 1) : 0);

        this.x = clamp(this.x, rect.x + MARGIN, rect.x + rect.width  - this.width  - MARGIN);
        this.y = clamp(this.y, rect.y + MARGIN, rect.y + rect.height - this.height - MARGIN);
    }

    public Point center() {
        return new Point(x + width / 2, y + height / 2);
    }

    public Rectangle toRect() {
        return new Rectangle(x, y, width, height);
    }


    private static double randRange(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    private static int clamp(int val, int min, int max) {
        if (min > max) return min;
        return Math.max(min, Math.min(max, val));
    }
}