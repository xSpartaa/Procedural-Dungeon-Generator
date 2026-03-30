package main;


import dungeon.Node;

import javax.swing.*;
import java.awt.*;

public class DungeonPanel  extends JPanel implements Runnable {
    public Thread gameThread;
    public int FPS = 60;

    public DungeonPanel() {
        launch();
    }

    public void launch() {
        gameThread = new Thread(this, "game-loop");
        gameThread.start();

        Node node = new Node(new Rectangle(0,0,100,100));
        int count = node.countNodes();
        System.out.println("count" +count);
        run();
    }



    // ═════════════════════════════════════════════════════════════════════
    //  GAME LOOP
    // ═════════════════════════════════════════════════════════════════════

    public long globalTimer = 0;

    public void run() {
        final double drawInterval = 1_000_000_000.0 / FPS;
        double delta    = 0;
        long   lastTime = System.nanoTime();
        long   fpsTimer = 0;
        int    drawCount = 0;

        while (gameThread != null) {
            long now     = System.nanoTime();
            long elapsed = now - lastTime;
            lastTime     = now;

            delta       += elapsed / drawInterval;
            globalTimer += elapsed;
            fpsTimer    += elapsed;

            if (delta >= 1) {
                update();
                delta--;
                drawCount++;
            }

            if (fpsTimer >= 1_000_000_000L) {
                drawCount = 0;
                fpsTimer  = 0;
            }
        }
    }

    public void update() {

    }

}
