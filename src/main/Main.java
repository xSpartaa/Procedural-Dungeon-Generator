package main;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static JFrame window;

    public static void main(String[] args) {
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        window.setTitle("Dungeon Generator");
        window.setPreferredSize(new Dimension(1000, 900));

        DungeonPanel dungeonPanel = new DungeonPanel();
        window.add(dungeonPanel);

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}