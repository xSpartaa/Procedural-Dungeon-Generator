package dungeon;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DungeonGenerator {

    private static final int MIN_SIZE   = 18;
    private static final int ITERATIONS = 5;

    public final Node        root;
    public final List<Node>  leaves    = new ArrayList<>();
    public final List<Corridor> corridors = new ArrayList<>();

    public Node entrance, boss, miniBoss;

    // Ensemble des paires déjà reliées (indices dans 'leaves')
    // On utilise les indices une fois les feuilles collectées
    private final Set<Long> pairSet = new HashSet<>();

    public DungeonGenerator(Rectangle worldRect) {
        root = new Node(worldRect);
        generate();
    }

    private void generate() {
        splitRecursive(root, ITERATIONS);
        root.createRooms();
        root.collectLeaves(leaves);
        // Indexer les feuilles pour la déduplication
        Map<Node,Integer> idx = new IdentityHashMap<>();
        for (int i=0;i<leaves.size();i++) idx.put(leaves.get(i), i);
        buildCorridors(root, idx);
        assignSpecialRooms();
    }

    private void splitRecursive(Node node, int depth) {
        if (depth<=0) return;
        node.generate(MIN_SIZE);
        if (node.left !=null) splitRecursive(node.left,  depth-1);
        if (node.right!=null) splitRecursive(node.right, depth-1);
    }

    private void buildCorridors(Node node, Map<Node,Integer> idx) {
        if (node==null || node.isLeaf()) return;
        buildCorridors(node.left,  idx);
        buildCorridors(node.right, idx);

        Node lLeaf = pickClosest(node.left,  node.right);
        Node rLeaf = pickClosest(node.right, node.left);
        if (lLeaf==null || rLeaf==null) return;
        if (lLeaf==rLeaf) return;
        if (lLeaf.room==null || rLeaf.room==null) return;

        Integer li = idx.get(lLeaf), ri = idx.get(rLeaf);
        if (li==null || ri==null) return;

        // Clé symétrique avec les indices de feuilles (stables)
        int a=Math.min(li,ri), b=Math.max(li,ri);
        long key = ((long)a<<32)|b;
        if (pairSet.add(key)) {
            corridors.add(new Corridor(lLeaf.room, rLeaf.room));
        }
    }

    private Node pickClosest(Node subtree, Node other) {
        if (subtree==null) return null;
        List<Node> cands=new ArrayList<>();
        subtree.collectLeaves(cands);
        cands.removeIf(n->n.room==null);
        if (cands.isEmpty()) return null;
        Point target=other.getCenter();
        return cands.stream()
                .min(Comparator.comparingDouble(n->dist(n.room.center(),target)))
                .orElse(null);
    }

    private void assignSpecialRooms() {
        if (leaves.isEmpty()) return;
        Point origin=new Point(0,0);
        entrance=leaves.stream().filter(n->n.room!=null)
                .min(Comparator.comparingDouble(n->dist(n.getCenter(),origin)))
                .orElse(leaves.get(0));
        entrance.room.type=RoomType.ENTRANCE;

        Point ec=entrance.getCenter();
        boss=leaves.stream().filter(n->n.room!=null && n!=entrance)
                .max(Comparator.comparingDouble(n->dist(n.getCenter(),ec)))
                .orElse(null);
        if (boss==null) return;

        Room oldRoom=boss.room;
        boss.createBossRoom();
        Room newRoom=boss.room;
        // Mettre à jour les corridors qui référençaient l'ancienne instance
        for (int i=0;i<corridors.size();i++) {
            Corridor c=corridors.get(i);
            if (c.roomA==oldRoom||c.roomB==oldRoom) {
                Room a=(c.roomA==oldRoom)?newRoom:c.roomA;
                Room b=(c.roomB==oldRoom)?newRoom:c.roomB;
                corridors.set(i,new Corridor(a,b));
            }
        }

        Point mid=mid(ec,boss.getCenter());
        miniBoss=leaves.stream().filter(n->n.room!=null&&n!=entrance&&n!=boss)
                .min(Comparator.comparingDouble(n->dist(n.getCenter(),mid)))
                .orElse(null);
        if (miniBoss!=null) miniBoss.room.type=RoomType.MINI_BOSS;
    }

    private static double dist(Point a,Point b){double dx=a.x-b.x,dy=a.y-b.y;return dx*dx+dy*dy;}
    private static Point  mid (Point a,Point b){return new Point((a.x+b.x)/2,(a.y+b.y)/2);}
}