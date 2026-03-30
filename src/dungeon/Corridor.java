package dungeon;

import java.awt.*;


public record Corridor(Point from, Point to, boolean hFirst) {
    public static final int THICKNESS = 2;

    public void draw(Graphics2D g2, int scale) {
        int x1 = from.x * scale;
        int y1 = from.y * scale;
        int x2 = to.x * scale;
        int y2 = to.y * scale;
        int t = THICKNESS * scale;

        int midX = hFirst ? x2 : x1;
        int midY = hFirst ? y1 : y2;

        // Segment 1
        int sx = Math.min(x1, midX);
        int sy = Math.min(y1, midY);
        int sw = Math.abs(midX - x1) + t;
        int sh = Math.abs(midY - y1) + t;
        g2.fillRect(sx, sy, sw, sh);

        // Segment 2
        sx = Math.min(midX, x2);
        sy = Math.min(midY, y2);
        sw = Math.abs(x2 - midX) + t;
        sh = Math.abs(y2 - midY) + t;
        g2.fillRect(sx, sy, sw, sh);
    }
}