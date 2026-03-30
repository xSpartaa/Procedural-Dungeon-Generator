package dungeon;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;


public class Corridor {

    public static final int THICKNESS = 2;
    public final int x1, y1;
    public final int x2, y2;
    public final int midX, midY;

    public Corridor(Room from, Room to) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Chevauchement horizontal (les deux salles sont sur la même bande en X)
        int overlapXStart = Math.max(from.x, to.x);
        int overlapXEnd   = Math.min(from.x + from.width, to.x + to.width);

        // Chevauchement vertical (les deux salles sont sur la même bande en Y)
        int overlapYStart = Math.max(from.y, to.y);
        int overlapYEnd   = Math.min(from.y + from.height, to.y + to.height);

        boolean hasOverlapX = overlapXEnd - overlapXStart >= 2;
        boolean hasOverlapY = overlapYEnd - overlapYStart >= 2;

        if (hasOverlapY) {
            // Les salles sont côte à côte horizontalement → couloir droit horizontal
            int sharedY = rng.nextInt(overlapYStart + 1, overlapYEnd - 1);
            // Point sur le bord droit de 'from' ou gauche selon position relative
            if (from.x + from.width <= to.x) {
                // from est à gauche de to
                x1 = from.x + from.width; y1 = sharedY;
                x2 = to.x;               y2 = sharedY;
            } else {
                // from est à droite de to
                x1 = from.x; y1 = sharedY;
                x2 = to.x + to.width; y2 = sharedY;
            }
            midX = x2; midY = y1; // L plat (en fait c'est droit)

        } else if (hasOverlapX) {
            // Les salles sont l'une au-dessus de l'autre → couloir droit vertical
            int sharedX = rng.nextInt(overlapXStart + 1, overlapXEnd - 1);
            if (from.y + from.height <= to.y) {
                // from est au-dessus de to
                x1 = sharedX; y1 = from.y + from.height;
                x2 = sharedX; y2 = to.y;
            } else {
                x1 = sharedX; y1 = from.y;
                x2 = sharedX; y2 = to.y + to.height;
            }
            midX = x1; midY = y2;

        } else {
            // Pas de chevauchement → couloir en L
            // On sort de 'from' par le côté le plus proche de 'to'
            Point fc = from.center();
            Point tc = to.center();

            // Choisir le point de départ sur le bord de 'from'
            if (Math.abs(tc.x - fc.x) >= Math.abs(tc.y - fc.y)) {
                // Déplacement principalement horizontal
                x1 = (tc.x > fc.x) ? from.x + from.width : from.x;
                y1 = clamp(tc.y, from.y + 1, from.y + from.height - 1);
                x2 = (tc.x > fc.x) ? to.x : to.x + to.width;
                y2 = clamp(y1, to.y + 1, to.y + to.height - 1);
                midX = x2; midY = y1;
            } else {
                // Déplacement principalement vertical
                x1 = clamp(tc.x, from.x + 1, from.x + from.width - 1);
                y1 = (tc.y > fc.y) ? from.y + from.height : from.y;
                x2 = clamp(x1, to.x + 1, to.x + to.width - 1);
                y2 = (tc.y > fc.y) ? to.y : to.y + to.height;
                midX = x1; midY = y2;
            }
        }
    }

    public void draw(Graphics2D g2, int scale, int thickness) {
        int px1 = x1*scale, py1 = y1*scale;
        int px2 = x2*scale, py2 = y2*scale;
        int pmx = midX*scale, pmy = midY*scale;
        int t   = thickness*scale;

        int sx = Math.min(px1, pmx) - t/2;
        int sy = Math.min(py1, pmy) - t/2;
        int sw = Math.abs(pmx - px1) + t;
        int sh = Math.abs(pmy - py1) + t;
        g2.fillRect(sx, sy, sw, sh);

        sx = Math.min(pmx, px2) - t/2;
        sy = Math.min(pmy, py2) - t/2;
        sw = Math.abs(px2 - pmx) + t;
        sh = Math.abs(py2 - pmy) + t;
        g2.fillRect(sx, sy, sw, sh);
    }

    private static int clamp(int v, int min, int max) {
        if (min > max) return (min + max) / 2;
        return Math.max(min, Math.min(max, v));
    }
}