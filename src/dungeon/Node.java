package dungeon;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nœud BSP. Responsabilité : découpage récursif + salles dans les feuilles.
 * Les couloirs sont gérés par DungeonGenerator (graphe MST).
 */
public class Node {

    public final Rectangle rect;
    public Node left, right;
    public Room room;

    public Node(Rectangle rect) { this.rect = rect; }

    // --- Split BSP ---

    public void generate(int minSize) {
        if (left != null || right != null) return;
        boolean canH = rect.height >= minSize * 2;
        boolean canV = rect.width  >= minSize * 2;
        if (!canH && !canV) return;
        Rectangle[] halves = split(minSize, canH, canV);
        if (halves == null) return;
        left  = new Node(halves[0]);
        right = new Node(halves[1]);
        left.generate(minSize);
        right.generate(minSize);
    }

    private Rectangle[] split(int minSize, boolean canH, boolean canV) {
        boolean splitH;
        if (canH && canV) {
            double ratio = (double) rect.width / rect.height;
            if (ratio > 1.25)          splitH = false;
            else if (1.0/ratio > 1.25) splitH = true;
            else                       splitH = ThreadLocalRandom.current().nextBoolean();
        } else { splitH = canH; }

        double pct = 0.38 + ThreadLocalRandom.current().nextDouble() * 0.24;

        if (splitH) {
            int h1 = (int)(rect.height * pct), h2 = rect.height - h1;
            if (h1 < minSize || h2 < minSize) return null;
            return new Rectangle[]{
                    new Rectangle(rect.x, rect.y, rect.width, h1),
                    new Rectangle(rect.x, rect.y + h1, rect.width, h2)};
        } else {
            int w1 = (int)(rect.width * pct), w2 = rect.width - w1;
            if (w1 < minSize || w2 < minSize) return null;
            return new Rectangle[]{
                    new Rectangle(rect.x, rect.y, w1, rect.height),
                    new Rectangle(rect.x + w1, rect.y, w2, rect.height)};
        }
    }

    // --- Salles ---

    public void createRooms() {
        if (isLeaf()) { room = new Room(rect.x,rect.y,rect.width, rect.height); }
        else {
            if (left  != null) left.createRooms();
            if (right != null) right.createRooms();
        }
    }

    public void createBossRoom() {
        if (isLeaf()) { room = new Room(rect.x,rect.y,rect.width,rect.height); room.type = RoomType.BOSS; }
    }

    // --- Utils ---

    public boolean isLeaf() { return left == null && right == null; }

    public Point getCenter() {
        return room != null ? room.center()
                : new Point(rect.x + rect.width/2, rect.y + rect.height/2);
    }

    public void collectLeaves(List<Node> out) {
        if (isLeaf()) { out.add(this); return; }
        if (left  != null) left.collectLeaves(out);
        if (right != null) right.collectLeaves(out);
    }
}