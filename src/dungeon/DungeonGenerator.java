package dungeon;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Orchestre la génération complète du donjon :
 *  1. Split BSP
 *  2. Création des salles dans les feuilles
 *  3. Couloirs entre salles adjacentes
 *  4. Attribution des types spéciaux (entrée, mini-boss, boss)
 */
public class DungeonGenerator {

    private static final int MIN_SIZE    = 14; // taille minimale d'un conteneur
    private static final int ITERATIONS  = 6;  // profondeur max de split

    public final Node root;
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

        // 2. Collecter les feuilles, créer les salles normales
        root.collectLeaves(leaves);
        for (Node leaf : leaves) {
            leaf.createRooms();
        }

        // 3. Couloirs (relie les sous-arbres gauche / droit à chaque niveau)
        root.connectRooms();
        root.collectCorridors(corridors);

        // 4. Choisir les salles spéciales
        assignSpecialRooms();
    }

    /** Split récursif limité par le nombre d'itérations et la taille minimale. */
    private void splitRecursive(Node node, int depth) {
        if (depth <= 0) return;
        node.generate(MIN_SIZE);
        if (node.left  != null) splitRecursive(node.left,  depth - 1);
        if (node.right != null) splitRecursive(node.right, depth - 1);
    }

    // ---------------------------------------------------------------
    // Salles spéciales
    // ---------------------------------------------------------------

    private void assignSpecialRooms() {
        if (leaves.isEmpty()) return;

        // --- Salle d'entrée : feuille dont le centre est le plus proche du coin haut-gauche ---
        Point origin = new Point(0, 0);
        entrance = leaves.stream()
                .min(Comparator.comparingDouble(n -> distSq(centerOf(n), origin)))
                .orElse(leaves.get(0));
        entrance.room.type = RoomType.ENTRANCE;

        // --- Salle de boss : feuille la plus loin de l'entrée (distance euclidienne centre-centre) ---
        Point entranceCenter = centerOf(entrance);
        boss = leaves.stream()
                .filter(n -> n != entrance)
                .max(Comparator.comparingDouble(n -> distSq(centerOf(n), entranceCenter)))
                .orElse(leaves.get(leaves.size() - 1));

        // Recréer la salle de boss (plus grande)
        boss.createBossRoom();

        // --- Salle de mini-boss : feuille la plus proche du milieu du chemin entrée→boss ---
        Point mid = midPoint(entranceCenter, centerOf(boss));
        miniBoss = leaves.stream()
                .filter(n -> n != entrance && n != boss)
                .min(Comparator.comparingDouble(n -> distSq(centerOf(n), mid)))
                .orElse(null);
        if (miniBoss != null) {
            miniBoss.room.type = RoomType.MINI_BOSS;
        }

        // --- L'entrée n'a qu'un seul couloir : on garde uniquement le corridor le plus proche ---
        // (déjà géré structurellement car l'entrée est une feuille dans l'arbre BSP)
    }

    // ---------------------------------------------------------------
    // Utilitaires géométriques
    // ---------------------------------------------------------------

    private static Point centerOf(Node n) {
        return (n.room != null) ? n.room.center() : n.getCenter();
    }

    private static double distSq(Point a, Point b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private static Point midPoint(Point a, Point b) {
        return new Point((a.x + b.x) / 2, (a.y + b.y) / 2);
    }
}