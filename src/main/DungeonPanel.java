package main;

import dungeon.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.List;

public class DungeonPanel extends JPanel implements Runnable {

    private static final int S    = 10;
    private static final int WW   = 116;
    private static final int WH   = 106;
    private static final int CT   = Corridor.THICKNESS;
    private static final int WALL = 7;

    // Palette
    private static final Color BG        = new Color(8,   8,  18);
    private static final Color GRID_C    = new Color(14, 18,  30);
    private static final Color COR_OUTER = new Color(20, 14,   5);
    private static final Color COR_STONE = new Color(65, 48,  20);
    private static final Color COR_FLOOR = new Color(112, 84, 40);
    private static final Color COR_SHINE = new Color(158,122, 62);
    private static final Color RM_OUTER  = new Color(20,  5,   5);
    private static final Color RM_STONE  = new Color(52, 12,  12);
    private static final Color RM_FLOOR  = new Color(142, 24, 24);
    private static final Color RM_LIGHT  = new Color(178, 42, 42);
    private static final Color RM_EDGE   = new Color(212, 58, 58);
    private static final Color EN_OUTER  = new Color(5,  22,   8);
    private static final Color EN_STONE  = new Color(12, 52,  18);
    private static final Color EN_FLOOR  = new Color(34, 150, 50);
    private static final Color EN_LIGHT  = new Color(50, 190, 68);
    private static final Color EN_EDGE   = new Color(80, 238, 98);
    private static final Color MB_OUTER  = new Color(36, 18,   2);
    private static final Color MB_STONE  = new Color(68, 34,   4);
    private static final Color MB_FLOOR  = new Color(182, 98,  8);
    private static final Color MB_LIGHT  = new Color(218,128, 24);
    private static final Color MB_EDGE   = new Color(250,168, 42);
    private static final Color BS_OUTER  = new Color(18,  4,  32);
    private static final Color BS_STONE  = new Color(38,  8,  62);
    private static final Color BS_FLOOR  = new Color(96,  26, 158);
    private static final Color BS_LIGHT  = new Color(128, 46, 202);
    private static final Color BS_EDGE   = new Color(175, 86, 248);
    private static final Color DR_GOLD   = new Color(200,168, 88);
    private static final Color DR_LITE   = new Color(238,206,130);
    private static final Color DR_DARK   = new Color(8,   5,   1);
    private static final Color ID_COLOR  = new Color(255,255,200,200);

    private record Seg(boolean horiz, int fixed, int from, int to) {}
    // Une porte = intersection d'un segment de couloir avec un mur de salle
    private record DoorHit(int px, int py, boolean horizWall, int wallCoord, Room room) {}

    private DungeonData data;
    private List<Seg>     segs     = new ArrayList<>();
    private List<DoorHit> doorHits = new ArrayList<>();

    private double zoom = 1.0;
    private int panX = 20, panY = 20, dsx, dsy, psx, psy;

    public DungeonPanel() {
        setBackground(BG);
        setFocusable(true);
        regenerate();
        setupControls();
        new Thread(this).start();
    }

    private void regenerate() {
        data     = new DungeonGenerator(WW, WH).generate();
        segs     = buildSegments();
        doorHits = computeDoors();
        repaint();
    }

    // ================================================================
    // SEGMENTS — collecte + fusion conditionnelle
    // ================================================================
    private List<Seg> buildSegments() {
        LinkedHashMap<String,int[]> rawMap = new LinkedHashMap<>();
        for (Corridor c : data.corridors) {
            addRaw(rawMap, c.ax, c.ay, c.midX, c.midY);
            addRaw(rawMap, c.midX, c.midY, c.bx, c.by);
        }

        Map<Boolean, TreeMap<Integer, List<int[]>>> byLine = new HashMap<>();
        byLine.put(true,  new TreeMap<>());
        byLine.put(false, new TreeMap<>());
        for (int[] seg : rawMap.values()) {
            boolean h = seg[0]==1;
            byLine.get(h).computeIfAbsent(seg[1], k->new ArrayList<>())
                    .add(new int[]{seg[2], seg[3]});
        }

        List<Seg> result = new ArrayList<>();
        Room[] rooms = data.rooms;
        for (boolean h : new boolean[]{true,false}) {
            for (Map.Entry<Integer,List<int[]>> e : byLine.get(h).entrySet()) {
                int fixed = e.getKey();
                List<int[]> ivs = e.getValue();
                ivs.sort(Comparator.comparingInt(i->i[0]));
                int[] cur = null;
                for (int[] iv : ivs) {
                    if (iv[0]>=iv[1]) continue;
                    if (cur==null) { cur=new int[]{iv[0],iv[1]}; continue; }
                    if (iv[0]<=cur[1]+1) {
                        int[] cand={cur[0],Math.max(cur[1],iv[1])};
                        boolean cross=false;
                        for (Room r:rooms) {
                            if (h?Corridor.crosses(cand[0],fixed,cand[1],fixed,r)
                                    :Corridor.crosses(fixed,cand[0],fixed,cand[1],r)){cross=true;break;}
                        }
                        if (!cross) cur=cand;
                        else { result.add(new Seg(h,fixed,cur[0],cur[1])); cur=new int[]{iv[0],iv[1]}; }
                    } else { result.add(new Seg(h,fixed,cur[0],cur[1])); cur=new int[]{iv[0],iv[1]}; }
                }
                if (cur!=null) result.add(new Seg(h,fixed,cur[0],cur[1]));
            }
        }
        return result;
    }

    private static void addRaw(LinkedHashMap<String,int[]> m, int x1,int y1,int x2,int y2) {
        if (x1==x2&&y1==y2) return;
        if (y1==y2) {
            int a=Math.min(x1,x2),b=Math.max(x1,x2);
            m.putIfAbsent("H,"+y1+","+a+","+b, new int[]{1,y1,a,b});
        } else if (x1==x2) {
            int a=Math.min(y1,y2),b=Math.max(y1,y2);
            m.putIfAbsent("V,"+x1+","+a+","+b, new int[]{0,x1,a,b});
        }
    }

    // ================================================================
    // PORTES — intersection segments ↔ murs
    // ================================================================
    private List<DoorHit> computeDoors() {
        List<DoorHit> hits = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Room[] rooms = data.rooms;
        int half = CT/2;

        for (Seg seg : segs) {
            for (Room r : rooms) {
                int rx=r.x,ry=r.y,rw=r.width,rh=r.height;
                if (seg.horiz()) {
                    int cy=seg.fixed(),xMin=seg.from(),xMax=seg.to();
                    if (xMin<rx&&xMax>rx&&cy>ry+half&&cy<ry+rh-half)
                        hit(hits,seen,rx*S,cy*S,false,rx*S,r);
                    if (xMin<rx+rw&&xMax>rx+rw&&cy>ry+half&&cy<ry+rh-half)
                        hit(hits,seen,(rx+rw)*S,cy*S,false,(rx+rw)*S,r);
                } else {
                    int cx=seg.fixed(),yMin=seg.from(),yMax=seg.to();
                    if (yMin<ry&&yMax>ry&&cx>rx+half&&cx<rx+rw-half)
                        hit(hits,seen,cx*S,ry*S,true,ry*S,r);
                    if (yMin<ry+rh&&yMax>ry+rh&&cx>rx+half&&cx<rx+rw-half)
                        hit(hits,seen,cx*S,(ry+rh)*S,true,(ry+rh)*S,r);
                }
            }
        }
        return hits;
    }

    private static void hit(List<DoorHit> hits,Set<String> seen,
                            int px,int py,boolean hw,int wc,Room r) {
        // Clé unique par salle + côté (wallCoord) pour éviter portes empilées
        String key = System.identityHashCode(r)+","+wc+","+(hw?px:py);
        if (seen.add(key)) hits.add(new DoorHit(px,py,hw,wc,r));
    }

    // ================================================================
    // PAINT
    // ================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data==null) return;
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.translate(panX,panY);
        g2.scale(zoom,zoom);

        drawGrid(g2);
        drawCorridors(g2);
        drawRooms(g2);
        drawDoors(g2);
        drawLabels(g2);
        drawRoomIds(g2);

        g2.dispose();
        drawLegend((Graphics2D)g);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(GRID_C); g2.setStroke(new BasicStroke(0.4f));
        for(int x=0;x<=WW;x+=5) g2.drawLine(x*S,0,x*S,WH*S);
        for(int y=0;y<=WH;y+=5) g2.drawLine(0,y*S,WW*S,y*S);
    }

    private void drawCorridors(Graphics2D g2) {
        int t=CT*S, sol=t-4;

        // Clip pour ne pas déborder dans les salles
        Shape clip = g2.getClip();
        Area corArea = new Area(new Rectangle(0,0,WW*S+100,WH*S+100));
        for (Room r:data.rooms)
            corArea.subtract(new Area(new Rectangle(r.x*S+WALL,r.y*S+WALL,r.width*S-WALL*2,r.height*S-WALL*2)));
        g2.setClip(corArea);

        g2.setColor(COR_OUTER); for(Seg s:segs) ds(g2,s,t+8);
        g2.setColor(COR_STONE); for(Seg s:segs) ds(g2,s,t);
        g2.setColor(COR_FLOOR); for(Seg s:segs) ds(g2,s,sol);
        g2.setColor(COR_SHINE); g2.setStroke(new BasicStroke(1.5f));
        for(Seg s:segs){
            if(s.horiz()) g2.drawLine(s.from()*S,s.fixed()*S,s.to()*S,s.fixed()*S);
            else           g2.drawLine(s.fixed()*S,s.from()*S,s.fixed()*S,s.to()*S);
        }
        g2.setClip(clip);
    }

    private static void ds(Graphics2D g2,Seg s,int t){
        if(s.horiz()) g2.fillRect(s.from()*S-t/2,s.fixed()*S-t/2,(s.to()-s.from())*S+t,t);
        else           g2.fillRect(s.fixed()*S-t/2,s.from()*S-t/2,t,(s.to()-s.from())*S+t);
    }

    private void drawRooms(Graphics2D g2) {
        for (Room r:data.rooms) {
            Pal p=pal(r.type);
            int rx=r.x*S,ry=r.y*S,rw=r.width*S,rh=r.height*S,w=WALL,half=(CT*S)/2;
            g2.setColor(new Color(0,0,0,150)); g2.fillRect(rx+4,ry+5,rw,rh);
            g2.setColor(p.floor); g2.fillRect(rx,ry,rw,rh);
            g2.setColor(p.light); g2.fillRect(rx+rw/4,ry+rh/5,rw/2,rh*2/5);

            // Gaps des portes
            List<int[]> gT=new ArrayList<>(),gB=new ArrayList<>(),gL=new ArrayList<>(),gR=new ArrayList<>();
            for (DoorHit d:doorHits) {
                if (d.room()!=r) continue;
                int[] gap;
                if (d.horizWall()) {
                    gap=new int[]{d.px()-half,d.px()+half};
                    if(d.wallCoord()==ry) gT.add(gap); else gB.add(gap);
                } else {
                    gap=new int[]{d.py()-half,d.py()+half};
                    if(d.wallCoord()==rx) gL.add(gap); else gR.add(gap);
                }
            }

            g2.setColor(p.outer);
            wH(g2,rx,ry,rw,w,gT,rx,rx+rw);wH(g2,rx,ry+rh-w,rw,w,gB,rx,rx+rw);
            wV(g2,rx,ry,rh,w,gL,ry,ry+rh);wV(g2,rx+rw-w,ry,rh,w,gR,ry,ry+rh);
            g2.setColor(p.stone);
            wH(g2,rx+1,ry+1,rw-2,w-1,gT,rx,rx+rw);wH(g2,rx+1,ry+rh-w+1,rw-2,w-1,gB,rx,rx+rw);
            wV(g2,rx+1,ry+1,rh-2,w-1,gL,ry,ry+rh);wV(g2,rx+rw-w+1,ry+1,rh-2,w-1,gR,ry,ry+rh);
            g2.setColor(p.stone);
            g2.fillRect(rx,ry,w,w);g2.fillRect(rx+rw-w,ry,w,w);
            g2.fillRect(rx,ry+rh-w,w,w);g2.fillRect(rx+rw-w,ry+rh-w,w,w);
            g2.setColor(p.edge); g2.setStroke(new BasicStroke(1.5f));
            lH(g2,rx+w,ry+w,rw-w*2,gT);lH(g2,rx+w,ry+rh-w,rw-w*2,gB);
            lV(g2,rx+w,ry+w,rh-w*2,gL);lV(g2,rx+rw-w,ry+w,rh-w*2,gR);
        }
    }

    private void drawDoors(Graphics2D g2) {
        int half=(CT*S)/2,fw=5,w=WALL;
        for (DoorHit d:doorHits) {
            Room r=d.room();
            int rx=r.x*S,ry=r.y*S,rw=r.width*S,rh=r.height*S,wc=d.wallCoord();
            int ox,oy,ow,oh;
            if(d.horizWall()){
                ox=d.px()-half;ow=half*2;
                oy=(wc==ry)?ry:ry+rh-w;oh=w;
                ox=clamp(ox,rx,rx+rw-ow);
            } else {
                oy=d.py()-half;oh=half*2;
                ox=(wc==rx)?rx:rx+rw-w;ow=w;
                oy=clamp(oy,ry,ry+rh-oh);
            }
            g2.setColor(DR_DARK);   g2.fillRect(ox,oy,ow,oh);
            g2.setColor(COR_FLOOR); g2.fillRect(ox+1,oy+1,ow-2,oh-2);
            g2.setColor(DR_GOLD);
            if(d.horizWall()){g2.fillRect(ox-fw,oy-1,fw,oh+2);g2.fillRect(ox+ow,oy-1,fw,oh+2);}
            else{g2.fillRect(ox-1,oy-fw,ow+2,fw);g2.fillRect(ox-1,oy+oh,ow+2,fw);}
            g2.setColor(DR_LITE); g2.setStroke(new BasicStroke(1.4f));
            if(d.horizWall()){g2.drawLine(ox-fw,oy-1,ox-fw,oy+oh+1);g2.drawLine(ox+ow+fw-1,oy-1,ox+ow+fw-1,oy+oh+1);}
            else{g2.drawLine(ox-1,oy-fw,ox+ow+1,oy-fw);g2.drawLine(ox-1,oy+oh+fw-1,ox+ow+1,oy+oh+fw-1);}
            g2.setColor(COR_SHINE); g2.setStroke(new BasicStroke(1.1f));
            if(d.horizWall()) g2.drawLine(ox+3,wc,ox+ow-3,wc);
            else              g2.drawLine(wc,oy+3,wc,oy+oh-3);
        }
    }

    private void drawLabels(Graphics2D g2) {
        for (Room r:data.rooms) {
            String lbl=label(r.type); if(lbl.isEmpty()) continue;
            int rx=r.x*S,ry=r.y*S,rw=r.width*S,rh=r.height*S;
            g2.setFont(new Font("Monospaced",Font.BOLD,11));
            FontMetrics fm=g2.getFontMetrics();
            int lw=fm.stringWidth(lbl),cx=rx+(rw-lw)/2,cy=ry+(rh+fm.getAscent()-fm.getDescent())/2;
            g2.setColor(new Color(0,0,0,200));
            g2.fillRoundRect(cx-6,cy-fm.getAscent()-3,lw+12,fm.getHeight()+6,6,6);
            g2.setColor(new Color(245,245,255)); g2.drawString(lbl,cx,cy);
        }
    }

    /** Affiche l'ID de chaque salle en petit dans le coin haut-gauche. */
    private void drawRoomIds(Graphics2D g2) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        for (Room r : data.rooms) {
            int rx=r.x*S+4, ry=r.y*S+11;
            g2.setColor(new Color(0,0,0,160));
            g2.drawString("#"+r.id, rx+1, ry+1);
            g2.setColor(ID_COLOR);
            g2.drawString("#"+r.id, rx, ry);
        }
    }

    private void drawLegend(Graphics2D g2) {
        Object[][] rows={{EN_LIGHT,"Entrée"},{MB_LIGHT,"Mini-boss"},{BS_LIGHT,"Boss"},
                {RM_LIGHT,"Salle"},{COR_FLOOR,"Couloir"}};
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

    // ---- Wall helpers ----
    private static void wH(Graphics2D g2,int rx,int ry,int rw,int h,List<int[]> gaps,int mn,int mx){
        int cur=rx,end=rx+rw;
        for(int[] g:sorted(gaps)){int a=clamp(g[0],mn,mx),b=clamp(g[1],mn,mx);if(a>cur)g2.fillRect(cur,ry,a-cur,h);cur=Math.max(cur,b);}
        if(cur<end)g2.fillRect(cur,ry,end-cur,h);
    }
    private static void wV(Graphics2D g2,int rx,int ry,int rh,int w,List<int[]> gaps,int mn,int mx){
        int cur=ry,end=ry+rh;
        for(int[] g:sorted(gaps)){int a=clamp(g[0],mn,mx),b=clamp(g[1],mn,mx);if(a>cur)g2.fillRect(rx,cur,w,a-cur);cur=Math.max(cur,b);}
        if(cur<end)g2.fillRect(rx,cur,w,end-cur);
    }
    private static void lH(Graphics2D g2,int rx,int ry,int rw,List<int[]> gaps){
        int cur=rx,end=rx+rw;
        for(int[] g:sorted(gaps)){if(g[0]>cur)g2.drawLine(cur,ry,Math.min(g[0],end),ry);cur=Math.max(cur,g[1]);}
        if(cur<end)g2.drawLine(cur,ry,end,ry);
    }
    private static void lV(Graphics2D g2,int rx,int ry,int rh,List<int[]> gaps){
        int cur=ry,end=ry+rh;
        for(int[] g:sorted(gaps)){if(g[0]>cur)g2.drawLine(rx,cur,rx,Math.min(g[0],end));cur=Math.max(cur,g[1]);}
        if(cur<end)g2.drawLine(rx,cur,rx,end);
    }
    private static List<int[]> sorted(List<int[]> l){return l.stream().sorted(Comparator.comparingInt(a->a[0])).toList();}
    private static int clamp(int v,int lo,int hi){return lo>hi?lo:Math.max(lo,Math.min(hi,v));}

    private record Pal(Color outer,Color stone,Color floor,Color light,Color edge){}
    private static Pal pal(RoomType t){return switch(t){
        case ENTRANCE  ->new Pal(EN_OUTER,EN_STONE,EN_FLOOR,EN_LIGHT,EN_EDGE);
        case MINI_BOSS ->new Pal(MB_OUTER,MB_STONE,MB_FLOOR,MB_LIGHT,MB_EDGE);
        case BOSS      ->new Pal(BS_OUTER,BS_STONE,BS_FLOOR,BS_LIGHT,BS_EDGE);
        default        ->new Pal(RM_OUTER,RM_STONE,RM_FLOOR,RM_LIGHT,RM_EDGE);
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
    @Override public void run(){while(true){repaint();try{Thread.sleep(16);}catch(InterruptedException ignored){}}}
}