package dungeon;

import java.awt.Point;
import java.util.List;

/**
 * Couloir entre deux salles.
 * Choisit l'orientation du L qui évite de traverser d'autres salles.
 */
public class Corridor {
    public static final int THICKNESS = 3;

    public final Room roomA, roomB;
    public final int  ax, ay, bx, by, midX, midY;

    public Corridor(Room a, Room b, List<Room> allRooms) {
        this.roomA = a; this.roomB = b;
        Point ca = a.center(), cb = b.center();
        ax = ca.x; ay = ca.y; bx = cb.x; by = cb.y;

        int mid1X = bx, mid1Y = ay; // hFirst
        int mid2X = ax, mid2Y = by; // vFirst

        int col1 = score(ax,ay,mid1X,mid1Y,bx,by,a,b,allRooms);
        int col2 = score(ax,ay,mid2X,mid2Y,bx,by,a,b,allRooms);

        if (col1 <= col2) { midX=mid1X; midY=mid1Y; }
        else              { midX=mid2X; midY=mid2Y; }
    }

    private static int score(int ax,int ay,int mx,int my,int bx,int by,
                             Room a,Room b,List<Room> rooms) {
        int s=0;
        for (Room r:rooms) {
            if (r==a||r==b) continue;
            if (crosses(ax,ay,mx,my,r)||crosses(mx,my,bx,by,r)) s++;
        }
        return s;
    }

    public static boolean crosses(int x1,int y1,int x2,int y2,Room r) {
        int h=THICKNESS/2;
        int cx1,cy1,cx2,cy2;
        if (y1==y2) { cx1=Math.min(x1,x2);cy1=y1-h;cx2=Math.max(x1,x2);cy2=y1+h; }
        else if (x1==x2) { cx1=x1-h;cy1=Math.min(y1,y2);cx2=x1+h;cy2=Math.max(y1,y2); }
        else return false;
        return cx1<r.x+r.width-1 && cx2>r.x+1 && cy1<r.y+r.height-1 && cy2>r.y+1;
    }
}