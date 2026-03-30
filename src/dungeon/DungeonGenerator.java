package dungeon;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Génère le donjon complet :
 *  1. Split BSP
 *  2. Salles dans les feuilles
 *  3. Couloirs via MST sur les conteneurs BSP frères (voisinage structurel)
 *  4. Salles spéciales : entrée, mini-boss, boss
 *
 * STRATÉGIE DES COULOIRS :
 * Au lieu de connecter des sous-arbres aléatoires, on exploite la structure BSP :
 * à chaque nœud interne, on relie UNE feuille du sous-arbre gauche
 * à UNE feuille du sous-arbre droit. Ces deux feuilles sont voisines par
 * construction (elles partagent un bord commun). Cela garantit des couloirs courts
 * et locaux, sans traversées absurdes.
 */
public class DungeonGenerator {

    private static final int MIN_SIZE   = 15;
    private static final int ITERATIONS = 5;

    public final Node           root;
    public final List<Node>     leaves    = new ArrayList<>();
    public final List<Corridor> corridors = new ArrayList<>();

    public Node entrance;
    public Node boss;
    public Node miniBoss;

    public DungeonGenerator(Rectangle worldRect) {
        root = new Node(worldRect);
        generate();
    }

    // ---------------------------------------------------------------

    private void generate() {
        // 1. Split BSP
        splitRecursive(root, ITERATIONS);

        // 2. Salles normales dans les feuilles
        root.createRooms();
        root.collectLeaves(leaves);

        // 3. Couloirs structurels BSP
        buildCorridors(root);

        // 4. Salles spéciales
        assignSpecialRooms();
    }

    private void splitRecursive(Node node, int depth) {
        if (depth <= 0) return;
        node.generate(MIN_SIZE);
        if (node.left  != null) splitRecursive(node.left,  depth - 1);
        if (node.right != null) splitRecursive(node.right, depth - 1);
    }

    // ---------------------------------------------------------------
    // Couloirs : connexion structurelle BSP
    // ---------------------------------------------------------------

    /**
     * Pour chaque nœud interne, on prend LA FEUILLE LA PLUS PROCHE de la frontière
     * dans chaque sous-arbre et on les relie. Résultat : couloirs courts et locaux.
     */
    private void buildCorridors(Node node) {
        if (node == null || node.isLeaf()) return;

        // Connecter les deux sous-arbres enfants
        buildCorridors(node.left);
        buildCorridors(node.right);

        // Choisir les feuilles représentantes de chaque côté
        Node lLeaf = pickClosestLeaf(node.left,  node.right);
        Node rLeaf = pickClosestLeaf(node.right, node.left);

        if (lLeaf != null && rLeaf != null && lLeaf.room != null && rLeaf.room != null) {
            Point p1 = lLeaf.room.center();
            Point p2 = rLeaf.room.center();
            boolean hFirst = ThreadLocalRandom.current().nextBoolean();
            corridors.add(new Corridor(p1, p2, hFirst));
        }
    }

    /**
     * Parmi toutes les feuilles de 'subtree', retourne celle dont la salle
     * est la plus proche du centre de 'other'.
     */
    private Node pickClosestLeaf(Node subtree, Node other) {
        if (subtree == null) return null;
        List<Node> candidates = new ArrayList<>();
        subtree.collectLeaves(candidates);
        if (candidates.isEmpty()) return null;

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

        // Entrée : feuille la plus proche du coin haut-gauche
        Point origin = new Point(0, 0);
        entrance = leaves.stream()
                .min(Comparator.comparingDouble(n -> distSq(n.getCenter(), origin)))
                .orElse(leaves.get(0));
        entrance.room.type = RoomType.ENTRANCE;

        // Boss : feuille la plus loin de l'entrée
        Point ec = entrance.getCenter();
        boss = leaves.stream()
                .filter(n -> n != entrance)
                .max(Comparator.comparingDouble(n -> distSq(n.getCenter(), ec)))
                .orElse(leaves.get(leaves.size() - 1));
        boss.createBossRoom();

        // Mini-boss : feuille la plus proche du point médian entrée→boss
        Point mid = midPoint(ec, boss.getCenter());
        miniBoss = leaves.stream()
                .filter(n -> n != entrance && n != boss)
                .min(Comparator.comparingDouble(n -> distSq(n.getCenter(), mid)))
                .orElse(null);
        if (miniBoss != null) miniBoss.room.type = RoomType.MINI_BOSS;
    }

    // ---------------------------------------------------------------
    // Géométrie
    // ---------------------------------------------------------------

    private static double distSq(Point a, Point b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return dx*dx + dy*dy;
    }

    private static Point midPoint(Point a, Point b) {
        return new Point((a.x + b.x)/2, (a.y + b.y)/2);
    }
}