package main;

import dungeon.*;
import dungeon.Corridor.Side;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Rendu du donjon.
 *
 * PORTES : au lieu de les calculer depuis Corridor (source de bugs),
 * on scanne directement les pixels : pour chaque extrémité de couloir
 * (doorA et doorB), on cherche quelle salle est touchée et on perce
 * son mur à cet endroit précis.
 *
 * COULOIRS : 4 passes globales (ombre → mur → sol → reflet) pour
 * des intersections propres.
 */
public class DungeonPanel extends JPanel implements Runnable {

    private static final int S    = 10;  // px par unité logique
    private static final int WW   = 108; // monde largeur (unités)
    private static final int WH   = 98;  // monde hauteur
    private static final int CT   = Corridor.THICKNESS; // 3 unités = 30px
    private static final int WALL = 7;   // épaisseur mur salle (px)

    // Couloir
    private static final Color C_COR_OUT   = new Color(20, 14,  5);
    private static final Color C_COR_WALL  = new Color(65, 48, 20);
    private static final Color C_COR_FLOOR = new Color(112, 84, 40);
    private static final Color C_COR_SHINE = new Color(158,122, 62);

    // Salle normale
    private static final Color C_RM_OUT   = new Color(20,  5,  5);
    private static final Color C_RM_WALL  = new Color(52, 12, 12);
    private static final Color C_RM_FLOOR = new Color(142, 24, 24);
    private static final Color C_RM_LIGHT = new Color(178, 42, 42);
    private static final Color C_RM_EDGE  = new Color(212, 58, 58);

    // Entrée
    private static final Color C_EN_OUT   = new Color(5,  22,  8);
    private static final Color C_EN_WALL  = new Color(12, 52, 18);
    private static final Color C_EN_FLOOR = new Color(34,150, 50);
    private static final Color C_EN_LIGHT = new Color(50,190, 68);
    private static final Color C_EN_EDGE  = new Color(80,238, 98);

    // Mini-boss
    private static final Color C_MB_OUT   = new Color(36, 18,  2);
    private static final Color C_MB_WALL  = new Color(68, 34,  4);
    private static final Color C_MB_FLOOR = new Color(182, 98,  8);
    private static final Color C_MB_LIGHT = new Color(218,128, 24);
    private static final Color C_MB_EDGE  = new Color(250,168, 42);

    // Boss
    private static final Color C_BS_OUT   = new Color(18,  4, 32);
    private static final Color C_BS_WALL  = new Color(38,  8, 62);
    private static final Color C_BS_FLOOR = new Color(96,  26,158);
    private static final Color C_BS_LIGHT = new Color(128, 46,202);
    private static final Color C_BS_EDGE  = new Color(175, 86,248);

    // Porte
    private static final Color C_DR_GOLD  = new Color(200,168, 88);
    private static final Color C_DR_LITE  = new Color(238,206,130);
    private static final Color C_DR_DARK  = new Color(8,   5,  1);

    private static final Color C_BG   = new Color(8, 8, 18);
    private static final Color C_GRID = new Color(14,18, 30);

    // ---- Une porte = point sur le bord d'une salle + côté ----
    // Calculée automatiquement depuis les extrémités des couloirs
    private record DoorSpec(
            Room  room,
            Side  side,       // côté percé
            int   along,      // centre de l'ouverture le long du mur (px absolu)
            int   wallPx      // coordonnée perpendiculaire du mur (px absolu)
    ){}

    private DungeonGenerator          dungeon;
    private final List<DoorSpec>      doors   = new ArrayList<>();

    private double zoom = 1.0;
    private int panX = 30, panY = 30, dsx, dsy, psx, psy;

    public DungeonPanel() {
        setBackground(C_BG);
        setFocusable(true);
        regenerate();
        setupControls();
        new Thread(this).start();
    }

    private void regenerate() {
        dungeon = new DungeonGenerator(new Rectangle(0, 0, WW, WH));
        doors.clear();
        computeDoors();
        repaint();
    }

    // ================================================================
    // CALCUL AUTOMATIQUE DES PORTES
    //
    // Pour chaque extrémité de couloir (doorA / doorB) :
    //   - On sait que ce point est sur le bord d'une salle
    //   - On cherche quelle salle contient ce point sur son bord
    //   - On détermine automatiquement le côté (TOP/BOTTOM/LEFT/RIGHT)
    //     en comparant le point aux bords de la salle
    //   - On crée un DoorSpec avec along = centre de l'ouverture
    //
    // Avantage : aucune logique de direction dans Corridor,
    // on lit juste où le couloir aboutit physiquement.
    // ================================================================
    /**
     * Calcule les portes directement depuis les données du Corridor.
     * Corridor garantit que doorA/doorB sont clampés dans les bornes de leur salle.
     * On utilise sideA/sideB pour connaître le côté sans scan.
     */
    private void computeDoors() {
        for (Corridor c : dungeon.corridors) {
            registerDoor(c.roomA, c.sideA, c.doorA);
            registerDoor(c.roomB, c.sideB, c.doorB);
        }
    }

    private void registerDoor(Room r, Corridor.Side side, Point door) {
        int rx=r.x*S, ry=r.y*S, rw=r.width*S, rh=r.height*S;
        int half = (CT*S)/2;
        int along, wallPx;

        // along = position EXACTE où le couloir touche le mur (pas de clamp)
        // C'est exactement door.x*S ou door.y*S selon le côté
        switch (side) {
            case LEFT   -> { along=door.y*S; wallPx=rx;    }
            case RIGHT  -> { along=door.y*S; wallPx=rx+rw; }
            case TOP    -> { along=door.x*S; wallPx=ry;    }
            default     -> { along=door.x*S; wallPx=ry+rh; }
        }

        // On n'ajoute la porte QUE si le couloir arrive bien dans le mur de la salle
        // (pas dans un coin, pas hors des bornes)
        boolean inBounds;
        if (side==Side.LEFT || side==Side.RIGHT)
            inBounds = along >= ry+half && along <= ry+rh-half;
        else
            inBounds = along >= rx+half && along <= rx+rw-half;

        if (!inBounds) return; // porte hors-mur → on ne la dessine pas

        final Side fs = side; final int fa = along;
        boolean dup = doors.stream().anyMatch(d ->
                d.room()==r && d.side()==fs && Math.abs(d.along()-fa) < 2);
        if (!dup) doors.add(new DoorSpec(r, side, along, wallPx));
    }

    // ================================================================
    // PAINT
    // ================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dungeon==null) return;
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.translate(panX, panY);
        g2.scale(zoom, zoom);

        drawGrid(g2);
        drawCorridors(g2);
        drawRooms(g2);
        drawDoors(g2);
        drawLabels(g2);

        g2.dispose();
        drawLegend((Graphics2D)g);
    }

    // ---- Grille ----
    private void drawGrid(Graphics2D g2) {
        g2.setColor(C_GRID); g2.setStroke(new BasicStroke(0.4f));
        for (int x=0;x<=WW;x+=5) g2.drawLine(x*S,0,x*S,WH*S);
        for (int y=0;y<=WH;y+=5) g2.drawLine(0,y*S,WW*S,y*S);
    }

    // ---- Couloirs : 4 passes globales ----
    private void drawCorridors(Graphics2D g2) {
        int t   = CT * S;      // épaisseur totale
        int sol = t - 2;       // sol intérieur

        // Passe 1 : ombre extérieure
        g2.setColor(C_COR_OUT);
        for (Corridor c : dungeon.corridors) {
            corSeg(g2,c.doorA.x*S,c.doorA.y*S,c.midX*S,c.midY*S,t+8);
            corSeg(g2,c.midX*S,c.midY*S,c.doorB.x*S,c.doorB.y*S,t+8);
            corBox(g2,c.midX*S,c.midY*S,t+8);
        }
        // Passe 2 : mur
        g2.setColor(C_COR_WALL);
        for (Corridor c : dungeon.corridors) {
            corSeg(g2,c.doorA.x*S,c.doorA.y*S,c.midX*S,c.midY*S,t);
            corSeg(g2,c.midX*S,c.midY*S,c.doorB.x*S,c.doorB.y*S,t);
            corBox(g2,c.midX*S,c.midY*S,t);
        }
        // Passe 3 : sol
        g2.setColor(C_COR_FLOOR);
        for (Corridor c : dungeon.corridors) {
            corSeg(g2,c.doorA.x*S,c.doorA.y*S,c.midX*S,c.midY*S,sol);
            corSeg(g2,c.midX*S,c.midY*S,c.doorB.x*S,c.doorB.y*S,sol);
            corBox(g2,c.midX*S,c.midY*S,sol);
        }
        // Passe 4 : reflet
        g2.setColor(C_COR_SHINE); g2.setStroke(new BasicStroke(1.5f));
        for (Corridor c : dungeon.corridors) {
            g2.drawLine(c.doorA.x*S,c.doorA.y*S,c.midX*S,c.midY*S);
            g2.drawLine(c.midX*S,c.midY*S,c.doorB.x*S,c.doorB.y*S);
        }
    }

    private static void corSeg(Graphics2D g2,int ax,int ay,int bx,int by,int t){
        g2.fillRect(Math.min(ax,bx)-t/2, Math.min(ay,by)-t/2,
                Math.abs(bx-ax)+t,   Math.abs(by-ay)+t);
    }
    private static void corBox(Graphics2D g2,int cx,int cy,int t){
        g2.fillRect(cx-t/2,cy-t/2,t,t);
    }

    // ---- Salles ----
    private void drawRooms(Graphics2D g2) {
        for (Node leaf : dungeon.leaves) {
            Room r = leaf.room; if (r==null) continue;
            drawRoom(g2, r);
        }
    }

    private void drawRoom(Graphics2D g2, Room r) {
        Pal p = pal(r.type);
        int rx=r.x*S, ry=r.y*S, rw=r.width*S, rh=r.height*S;
        int w = WALL;
        int half = (CT*S)/2;

        // Ombre portée
        g2.setColor(new Color(0,0,0,150));
        g2.fillRect(rx+4,ry+5,rw,rh);

        // Sol (recouvre l'intérieur des couloirs)
        g2.setColor(p.floor); g2.fillRect(rx,ry,rw,rh);
        g2.setColor(p.light); g2.fillRect(rx+rw/4,ry+rh/5,rw/2,rh*2/5);

        // Collecter les gaps de porte pour chaque côté de CETTE salle
        List<int[]> gT=new ArrayList<>(),gB=new ArrayList<>(),
                gL=new ArrayList<>(),gR=new ArrayList<>();
        for (DoorSpec d : doors) {
            if (d.room() != r) continue;
            int[] gap = {d.along()-half, d.along()+half};
            switch (d.side()) {
                case TOP    -> gT.add(gap);
                case BOTTOM -> gB.add(gap);
                case LEFT   -> gL.add(gap);
                case RIGHT  -> gR.add(gap);
            }
        }

        // Mur extérieur sombre
        g2.setColor(p.outer);
        wallH(g2,rx,      ry,      rw,w,gT,rx,rx+rw);
        wallH(g2,rx,      ry+rh-w, rw,w,gB,rx,rx+rw);
        wallV(g2,rx,      ry,      rh,w,gL,ry,ry+rh);
        wallV(g2,rx+rw-w, ry,      rh,w,gR,ry,ry+rh);

        // Pierre
        g2.setColor(p.wall);
        wallH(g2,rx+1,      ry+1,      rw-2,w-1,gT,rx,rx+rw);
        wallH(g2,rx+1,      ry+rh-w+1, rw-2,w-1,gB,rx,rx+rw);
        wallV(g2,rx+1,      ry+1,      rh-2,w-1,gL,ry,ry+rh);
        wallV(g2,rx+rw-w+1, ry+1,      rh-2,w-1,gR,ry,ry+rh);

        // Coins solides
        g2.setColor(p.wall);
        g2.fillRect(rx,ry,w,w); g2.fillRect(rx+rw-w,ry,w,w);
        g2.fillRect(rx,ry+rh-w,w,w); g2.fillRect(rx+rw-w,ry+rh-w,w,w);

        // Bordure lumineuse intérieure
        g2.setColor(p.edge); g2.setStroke(new BasicStroke(1.5f));
        lineH(g2,rx+w,   ry+w,   rw-w*2,gT);
        lineH(g2,rx+w,   ry+rh-w,rw-w*2,gB);
        lineV(g2,rx+w,   ry+w,   rh-w*2,gL);
        lineV(g2,rx+rw-w,ry+w,   rh-w*2,gR);
    }

    private static void wallH(Graphics2D g2,int rx,int ry,int rw,int h,
                              List<int[]> gaps,int mn,int mx){
        int cur=rx,end=rx+rw;
        for(int[] g:sorted(gaps)){int a=clamp(g[0],mn,mx),b=clamp(g[1],mn,mx);
            if(a>cur)g2.fillRect(cur,ry,a-cur,h);cur=Math.max(cur,b);}
        if(cur<end)g2.fillRect(cur,ry,end-cur,h);
    }
    private static void wallV(Graphics2D g2,int rx,int ry,int rh,int w,
                              List<int[]> gaps,int mn,int mx){
        int cur=ry,end=ry+rh;
        for(int[] g:sorted(gaps)){int a=clamp(g[0],mn,mx),b=clamp(g[1],mn,mx);
            if(a>cur)g2.fillRect(rx,cur,w,a-cur);cur=Math.max(cur,b);}
        if(cur<end)g2.fillRect(rx,cur,w,end-cur);
    }
    private static void lineH(Graphics2D g2,int rx,int ry,int rw,List<int[]> gaps){
        int cur=rx,end=rx+rw;
        for(int[] g:sorted(gaps)){if(g[0]>cur)g2.drawLine(cur,ry,Math.min(g[0],end),ry);cur=Math.max(cur,g[1]);}
        if(cur<end)g2.drawLine(cur,ry,end,ry);
    }
    private static void lineV(Graphics2D g2,int rx,int ry,int rh,List<int[]> gaps){
        int cur=ry,end=ry+rh;
        for(int[] g:sorted(gaps)){if(g[0]>cur)g2.drawLine(rx,cur,rx,Math.min(g[0],end));cur=Math.max(cur,g[1]);}
        if(cur<end)g2.drawLine(rx,cur,rx,end);
    }
    private static List<int[]> sorted(List<int[]> l){
        return l.stream().sorted(Comparator.comparingInt(a->a[0])).toList();}
    private static int clamp(int v,int lo,int hi){return lo>hi?lo:Math.max(lo,Math.min(hi,v));}

    // ---- Portes ----
    private void drawDoors(Graphics2D g2) {
        int half = (CT*S)/2;
        int fw   = 5; // épaisseur linteau

        for (DoorSpec d : doors) {
            Room r   = d.room();
            int  rx  = r.x*S, ry=r.y*S, rw=r.width*S, rh=r.height*S;
            int  ca  = d.along();
            int  wp  = d.wallPx();
            int  w   = WALL;

            // Rectangle du mur percé
            int ox,oy,ow,oh;
            switch (d.side()) {
                case LEFT   -> { ox=rx;      oy=ca-half; ow=w; oh=half*2; }
                case RIGHT  -> { ox=rx+rw-w; oy=ca-half; ow=w; oh=half*2; }
                case TOP    -> { ox=ca-half; oy=ry;      ow=half*2; oh=w; }
                default     -> { ox=ca-half; oy=ry+rh-w; ow=half*2; oh=w; }
            }
            // No clamp here: registerDoor already guarantees inBounds

            // Fond sombre + sol couloir
            g2.setColor(C_DR_DARK);   g2.fillRect(ox,oy,ow,oh);
            g2.setColor(C_COR_FLOOR); g2.fillRect(ox+1,oy+1,ow-2,oh-2);

            // Linteaux / montants dorés
            g2.setColor(C_DR_GOLD);
            switch (d.side()) {
                case LEFT,RIGHT -> {
                    g2.fillRect(ox-1,oy-fw,ow+2,fw);
                    g2.fillRect(ox-1,oy+oh,ow+2,fw);
                }
                case TOP,BOTTOM -> {
                    g2.fillRect(ox-fw,oy-1,fw,oh+2);
                    g2.fillRect(ox+ow,oy-1,fw,oh+2);
                }
            }

            // Arête lumineuse
            g2.setColor(C_DR_LITE); g2.setStroke(new BasicStroke(1.4f));
            switch (d.side()) {
                case LEFT,RIGHT -> {
                    g2.drawLine(ox-1,oy-fw,      ox+ow+1,oy-fw);
                    g2.drawLine(ox-1,oy+oh+fw-1, ox+ow+1,oy+oh+fw-1);
                }
                case TOP,BOTTOM -> {
                    g2.drawLine(ox-fw,oy-1,      ox-fw,     oy+oh+1);
                    g2.drawLine(ox+ow+fw-1,oy-1, ox+ow+fw-1,oy+oh+1);
                }
            }

            // Reflet dans le passage
            g2.setColor(C_COR_SHINE); g2.setStroke(new BasicStroke(1.1f));
            switch (d.side()) {
                case LEFT,RIGHT -> g2.drawLine(wp,oy+3,wp,oy+oh-3);
                case TOP,BOTTOM -> g2.drawLine(ox+3,wp,ox+ow-3,wp);
            }
        }
    }

    // ---- Labels ----
    private void drawLabels(Graphics2D g2) {
        for (Node leaf : dungeon.leaves) {
            Room r=leaf.room; if(r==null) continue;
            String lbl=label(r.type); if(lbl.isEmpty()) continue;
            int rx=r.x*S,ry=r.y*S,rw=r.width*S,rh=r.height*S;
            g2.setFont(new Font("Monospaced",Font.BOLD,11));
            FontMetrics fm=g2.getFontMetrics();
            int lw=fm.stringWidth(lbl),cx=rx+(rw-lw)/2;
            int cy=ry+(rh+fm.getAscent()-fm.getDescent())/2;
            g2.setColor(new Color(0,0,0,200));
            g2.fillRoundRect(cx-6,cy-fm.getAscent()-3,lw+12,fm.getHeight()+6,6,6);
            g2.setColor(new Color(245,245,255));
            g2.drawString(lbl,cx,cy);
        }
    }

    // ---- Légende ----
    private void drawLegend(Graphics2D g2) {
        Object[][] rows={{C_EN_LIGHT,"Entrée"},{C_MB_LIGHT,"Mini-boss"},
                {C_BS_LIGHT,"Boss"},{C_RM_LIGHT,"Salle"},{C_COR_FLOOR,"Couloir"}};
        int x=12,y=getHeight()-155,lh=23,bw=17,bh=17;
        g2.setColor(new Color(4,6,14,232)); g2.fillRoundRect(x-8,y-28,172,rows.length*lh+40,12,12);
        g2.setColor(new Color(44,58,105)); g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x-8,y-28,172,rows.length*lh+40,12,12);
        g2.setFont(new Font("Monospaced",Font.BOLD,12)); g2.setColor(new Color(158,172,210));
        g2.drawString("Légende   [R]=regen",x,y-8);
        g2.setFont(new Font("Monospaced",Font.PLAIN,11));
        for(int i=0;i<rows.length;i++){
            Color c=(Color)rows[i][0];
            g2.setColor(c.darker()); g2.fillRoundRect(x,y+i*lh,bw,bh,4,4);
            g2.setColor(c);          g2.fillRoundRect(x+1,y+i*lh+1,bw-3,bh/2,3,3);
            g2.setColor(c.brighter()); g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x,y+i*lh,bw,bh,4,4);
            g2.setColor(new Color(196,202,226)); g2.drawString((String)rows[i][1],x+bw+8,y+i*lh+bh-2);
        }
        g2.setFont(new Font("Monospaced",Font.PLAIN,10)); g2.setColor(new Color(58,72,112));
        g2.drawString("Molette=zoom  Drag=pan  DblClic=regen",12,getHeight()-6);
    }

    // ---- Helpers ----
    private record Pal(Color outer,Color wall,Color floor,Color light,Color edge){}
    private static Pal pal(RoomType t){return switch(t){
        case ENTRANCE  ->new Pal(C_EN_OUT,C_EN_WALL,C_EN_FLOOR,C_EN_LIGHT,C_EN_EDGE);
        case MINI_BOSS ->new Pal(C_MB_OUT,C_MB_WALL,C_MB_FLOOR,C_MB_LIGHT,C_MB_EDGE);
        case BOSS      ->new Pal(C_BS_OUT,C_BS_WALL,C_BS_FLOOR,C_BS_LIGHT,C_BS_EDGE);
        default        ->new Pal(C_RM_OUT,C_RM_WALL,C_RM_FLOOR,C_RM_LIGHT,C_RM_EDGE);
    };}
    private static String label(RoomType t){return switch(t){
        case ENTRANCE->"ENTRÉE";case MINI_BOSS->"MINI-BOSS";case BOSS->"★ BOSS ★";default->"";
    };}

    private void setupControls(){
        addMouseWheelListener(e->{zoom=Math.max(0.2,Math.min(8.0,zoom*(e.getWheelRotation()<0?1.12:0.89)));repaint();});
        addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){dsx=e.getX();dsy=e.getY();psx=panX;psy=panY;requestFocusInWindow();}
            public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)regenerate();}
        });
        addMouseMotionListener(new MouseMotionAdapter(){
            public void mouseDragged(MouseEvent e){panX=psx+(e.getX()-dsx);panY=psy+(e.getY()-dsy);repaint();}
        });
        addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_R)regenerate();}
        });
    }
    @Override public void run(){
        while(true){repaint();try{Thread.sleep(16);}catch(InterruptedException ignored){}}
    }
}