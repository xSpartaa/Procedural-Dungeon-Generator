package dungeon;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Génère un donjon par BSP et retourne un DungeonData.
 *
 * Usage :
 *   DungeonData data = new DungeonGenerator(120, 110).generate();
 */
public class DungeonGenerator {

    private static final int MIN_SIZE   = 20;
    private static final int ITERATIONS = 5;
    private static final int MARGIN     = 3; // espace entre salle et bord du conteneur

    private final int worldW, worldH;

    public DungeonGenerator(int worldW, int worldH) {
        this.worldW = worldW;
        this.worldH = worldH;
    }

    public DungeonData generate() {
        Room.resetIds();

        // 1. BSP split
        BSPNode root = new BSPNode(0, 0, worldW, worldH);
        split(root, ITERATIONS);

        // 2. Créer les salles dans les feuilles
        List<Room> rooms = new ArrayList<>();
        createRooms(root, rooms);

        // 3. Couloirs — déduplication par paire de Room
        List<Corridor> corridors = new ArrayList<>();
        Set<String> pairs = new HashSet<>();
        buildCorridors(root, rooms, corridors, pairs);

        // 4. Salles spéciales
        Room entrance = findEntrance(rooms);
        entrance.type = RoomType.ENTRANCE;

        Room boss = findBossByBFS(entrance, rooms, corridors);
        Room miniBoss = null;
        if (boss != null) {
            // Agrandir la salle boss
            enlargeRoom(boss);
            boss.type = RoomType.BOSS;
            miniBoss = findMiniBoss(entrance, boss, rooms, corridors);
            if (miniBoss != null) miniBoss.type = RoomType.MINI_BOSS;
        }

        return new DungeonData(
                rooms.toArray(new Room[0]),
                corridors.toArray(new Corridor[0]),
                entrance, boss, miniBoss
        );
    }

    // ================================================================
    // BSP
    // ================================================================

    private static class BSPNode {
        int x, y, w, h;
        BSPNode left, right;
        Room room; // non null seulement pour les feuilles

        BSPNode(int x,int y,int w,int h){ this.x=x;this.y=y;this.w=w;this.h=h; }

        boolean isLeaf(){ return left==null && right==null; }

        Point center(){
            if (room!=null) return room.center();
            return new Point(x+w/2, y+h/2);
        }
    }

    private void split(BSPNode node, int depth) {
        if (depth <= 0) return;
        if (node.w < MIN_SIZE*2 && node.h < MIN_SIZE*2) return;

        boolean canH = node.h >= MIN_SIZE*2;
        boolean canV = node.w >= MIN_SIZE*2;
        boolean horiz;
        if (canH && canV) {
            double r = (double)node.w/node.h;
            horiz = r>1.25 ? false : (1.0/r>1.25 ? true : rnd().nextBoolean());
        } else horiz = canH;

        double pct = 0.38 + rnd().nextDouble()*0.24;
        if (horiz) {
            int h1 = (int)(node.h*pct), h2 = node.h-h1;
            if (h1<MIN_SIZE||h2<MIN_SIZE) return;
            node.left  = new BSPNode(node.x, node.y,    node.w, h1);
            node.right = new BSPNode(node.x, node.y+h1, node.w, h2);
        } else {
            int w1 = (int)(node.w*pct), w2 = node.w-w1;
            if (w1<MIN_SIZE||w2<MIN_SIZE) return;
            node.left  = new BSPNode(node.x,    node.y, w1, node.h);
            node.right = new BSPNode(node.x+w1, node.y, w2, node.h);
        }
        split(node.left,  depth-1);
        split(node.right, depth-1);
    }

    private void createRooms(BSPNode node, List<Room> rooms) {
        if (node.isLeaf()) {
            int maxW = node.w - MARGIN*2, maxH = node.h - MARGIN*2;
            int minS = 8;
            maxW = Math.max(minS, maxW); maxH = Math.max(minS, maxH);
            int w = Room.clamp((int)(maxW * randRange(0.65,0.88)), minS, maxW);
            int h = Room.clamp((int)(maxH * randRange(0.65,0.88)), minS, maxH);
            int sx = node.x + MARGIN + (maxW-w>0 ? rnd().nextInt(maxW-w+1) : 0);
            int sy = node.y + MARGIN + (maxH-h>0 ? rnd().nextInt(maxH-h+1) : 0);
            sx = Room.clamp(sx, node.x+MARGIN, node.x+node.w-w-MARGIN);
            sy = Room.clamp(sy, node.y+MARGIN, node.y+node.h-h-MARGIN);
            Room r = new Room(sx, sy, w, h);
            node.room = r;
            rooms.add(r);
        } else {
            if (node.left  != null) createRooms(node.left,  rooms);
            if (node.right != null) createRooms(node.right, rooms);
        }
    }

    private void buildCorridors(BSPNode node, List<Room> allRooms,
                                List<Corridor> corridors, Set<String> pairs) {
        if (node == null || node.isLeaf()) return;
        buildCorridors(node.left,  allRooms, corridors, pairs);
        buildCorridors(node.right, allRooms, corridors, pairs);

        BSPNode lLeaf = pickClosest(node.left,  node.right);
        BSPNode rLeaf = pickClosest(node.right, node.left);
        if (lLeaf==null||rLeaf==null||lLeaf.room==null||rLeaf.room==null) return;
        if (lLeaf == rLeaf) return;

        String key = pairKey(lLeaf.room, rLeaf.room);
        if (pairs.add(key)) {
            corridors.add(new Corridor(lLeaf.room, rLeaf.room, allRooms));
        }
    }

    private BSPNode pickClosest(BSPNode subtree, BSPNode other) {
        if (subtree==null) return null;
        List<BSPNode> leaves = new ArrayList<>();
        collectLeaves(subtree, leaves);
        leaves.removeIf(n -> n.room==null);
        if (leaves.isEmpty()) return null;
        Point target = other.center();
        return leaves.stream()
                .min(Comparator.comparingDouble(n -> dist(n.room.center(), target)))
                .orElse(null);
    }

    private static void collectLeaves(BSPNode node, List<BSPNode> out) {
        if (node==null) return;
        if (node.isLeaf()) { out.add(node); return; }
        collectLeaves(node.left,  out);
        collectLeaves(node.right, out);
    }

    // ================================================================
    // Salles spéciales
    // ================================================================

    private static Room findEntrance(List<Room> rooms) {
        return rooms.stream()
                .min(Comparator.comparingDouble(r -> dist(r.center(), new Point(0,0))))
                .orElse(rooms.get(0));
    }

    private static Room findBossByBFS(Room start, List<Room> rooms, List<Corridor> corridors) {
        Map<Room,Integer> d = bfs(start, corridors);
        Point sc = start.center();
        Room best = null; int maxD=0; double maxE=0;
        for (Map.Entry<Room,Integer> e : d.entrySet()) {
            Room r=e.getKey(); if (r==start) continue;
            int bd=e.getValue(); double ed=dist(r.center(),sc);
            if (bd>maxD||(bd==maxD&&ed>maxE)){ maxD=bd;maxE=ed;best=r; }
        }
        return best;
    }

    private static Room findMiniBoss(Room entrance, Room boss,
                                     List<Room> rooms, List<Corridor> corridors) {
        Map<Room,Room> parent = new IdentityHashMap<>();
        Queue<Room> q = new LinkedList<>();
        parent.put(entrance, null); q.add(entrance);
        Map<Room,List<Room>> adj = buildAdj(corridors);
        while (!q.isEmpty()) {
            Room cur=q.poll(); if (cur==boss) break;
            for (Room nb:adj.getOrDefault(cur,List.of()))
                if (!parent.containsKey(nb)){ parent.put(nb,cur);q.add(nb); }
        }
        if (!parent.containsKey(boss)) return null;
        List<Room> path=new ArrayList<>();
        Room cur=boss; while(cur!=null){path.add(cur);cur=parent.get(cur);}
        Collections.reverse(path);
        if (path.size()<3) return null;
        Room mid=path.get(path.size()/2);
        return (mid==entrance||mid==boss)?null:mid;
    }

    private static void enlargeRoom(Room r) {
        // Agrandir légèrement la boss room (elle est déjà dans son conteneur,
        // on l'étend sans dépasser — on recalcule juste la taille)
        int extraW = (int)(r.width  * 0.25);
        int extraH = (int)(r.height * 0.25);
        r.x -= extraW/2; r.y -= extraH/2;
        r.width  += extraW; r.height += extraH;
    }

    // ================================================================
    // Helpers
    // ================================================================
    private static Map<Room,Integer> bfs(Room start, List<Corridor> corridors) {
        Map<Room,List<Room>> adj = buildAdj(corridors);
        Map<Room,Integer> dist = new IdentityHashMap<>();
        Queue<Room> q = new LinkedList<>();
        dist.put(start,0); q.add(start);
        while (!q.isEmpty()) {
            Room cur=q.poll(); int d=dist.get(cur);
            for (Room nb:adj.getOrDefault(cur,List.of()))
                if (!dist.containsKey(nb)){ dist.put(nb,d+1);q.add(nb); }
        }
        return dist;
    }

    private static Map<Room,List<Room>> buildAdj(List<Corridor> corridors) {
        Map<Room,List<Room>> adj = new IdentityHashMap<>();
        for (Corridor c:corridors) {
            adj.computeIfAbsent(c.roomA,k->new ArrayList<>()).add(c.roomB);
            adj.computeIfAbsent(c.roomB,k->new ArrayList<>()).add(c.roomA);
        }
        return adj;
    }

    private static String pairKey(Room a, Room b) {
        // Clé symétrique basée sur les IDs (stables et uniques)
        int lo=Math.min(a.id,b.id), hi=Math.max(a.id,b.id);
        return lo+"|"+hi;
    }

    private static double dist(Point a, Point b) {
        double dx=a.x-b.x,dy=a.y-b.y; return dx*dx+dy*dy;
    }
    private static double randRange(double a,double b) {
        return a+rnd().nextDouble()*(b-a);
    }
    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }
}