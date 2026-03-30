package dungeon;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Orchestre la génération du donjon :
 *  1. Split BSP
 *  2. Salles dans les feuilles
 *  3. Couloirs : à chaque nœud interne, on relie la feuille-bord gauche
 *     la plus proche de la frontière avec le sous-arbre droit
 *  4. Salles spéciales (entrée, mini-boss, boss)
 */
public class DungeonGenerator {

    private static final int MIN_SIZE   = 15;
    private static final int ITERATIONS = 5;

    public final Node           root;
    public final List<Node>     leaves    = new ArrayList<>();
    public final List<Corridor> corridors = new ArrayList<>();

    public Node entrance, boss, miniBoss;

    public DungeonGenerator(Rectangle worldRect) {
        root = new Node(worldRect);
        generate();
    }

    private void generate() {
        splitRecursive(root, ITERATIONS);
        root.createRooms();
        root.collectLeaves(leaves);
        buildCorridors(root);
        assignSpecialRooms();
    }

    private void splitRecursive(Node node, int depth) {
        if (depth <= 0) return;
        node.generate(MIN_SIZE);
        if (node.left  != null) splitRecursive(node.left,  depth - 1);
        if (node.right != null) splitRecursive(node.right, depth - 1);
    }

    // ---------------------------------------------------------------
    // Couloirs : voisinage structurel BSP + connexion bord-à-bord
    // ---------------------------------------------------------------

    private void buildCorridors(Node node) {
        if (node == null || node.isLeaf()) return;

        buildCorridors(node.left);
        buildCorridors(node.right);

        // Feuille du sous-arbre gauche la plus proche du sous-arbre droit
        Node lLeaf = pickClosestLeaf(node.left,  node.right);
        // Feuille du sous-arbre droit la plus proche du sous-arbre gauche
        Node rLeaf = pickClosestLeaf(node.right, node.left);

        if (lLeaf != null && rLeaf != null
                && lLeaf.room != null && rLeaf.room != null) {
            corridors.add(new Corridor(lLeaf.room, rLeaf.room));
        }
    }

    /**
     * Parmi les feuilles de 'subtree', retourne celle dont la salle
     * est la plus proche du centre de 'other'.
     */
    private Node pickClosestLeaf(Node subtree, Node other) {
        if (subtree == null) return null;
        List<Node> candidates = new ArrayList<>();
        subtree.collectLeaves(candidates);
        Point target = other.getCenter();
        return candidates.stream()
                .filter(n -> n.room != null)
                .min(Comparator.comparingDouble(n -> distSq(n.room.center(), target)))
                .orElse(null);
    }

    // ---------------------------------------------------------------
    // Salles spéciales
    // ---------------------------------------------------------------

    private void assignSpecialRooms() {
        if (leaves.isEmpty()) return;

        Point origin = new Point(0, 0);
        entrance = leaves.stream()
                .min(Comparator.comparingDouble(n -> distSq(n.getCenter(), origin)))
                .orElse(leaves.get(0));
        entrance.room.type = RoomType.ENTRANCE;

        Point ec = entrance.getCenter();
        boss = leaves.stream()
                .filter(n -> n != entrance)
                .max(Comparator.comparingDouble(n -> distSq(n.getCenter(), ec)))
                .orElse(leaves.get(leaves.size() - 1));
        boss.createBossRoom();

        Point mid = midPoint(ec, boss.getCenter());
        miniBoss = leaves.stream()
                .filter(n -> n != entrance && n != boss)
                .min(Comparator.comparingDouble(n -> distSq(n.getCenter(), mid)))
                .orElse(null);
        if (miniBoss != null) miniBoss.room.type = RoomType.MINI_BOSS;
    }

    // ---------------------------------------------------------------

    private static double distSq(Point a, Point b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return dx*dx + dy*dy;
    }
    private static Point midPoint(Point a, Point b) {
        return new Point((a.x+b.x)/2, (a.y+b.y)/2);
    }
}