package main;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static JFrame window;

    public static void main(String[] args) {
        window = new JFrame();
        window.setResizable(true);
        window.setTitle("Dungeon Generator");
        window.setMinimumSize(new Dimension(1920, 1080));

        DungeonPanel dungeonPanel = new DungeonPanel();
        window.add(dungeonPanel);
    }
}
