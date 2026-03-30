package dungeon;

import java.awt.*;

public class Node {
    Rectangle node;
    public Node left;
    public Node right;

    public Node(Rectangle node) {
        this.node = node;
        generate(10);
    }

    public void generate(int minSize) {
        // Si trop petit, on arrête
        if (node.width < minSize * 2 && node.height < minSize * 2) return;

        // On utilise ta logique de l'Exo 1 pour créer deux rectangles
        Rectangle[] splitRects = split(this.node);

        // On crée les enfants
        this.left = new Node(splitRects[0]);
        this.right = new Node(splitRects[1]);

    }

    public Rectangle[] split(Rectangle rect) {
        boolean isRatioOk = !(rect.height > 1.25*rect.width || rect.width > 1.25 * rect.height);
        int randChoice = (int) (Math.random()*10);
        System.out.println(randChoice);
        int randRatio = 30 + (int) (Math.random()*10*4);
        if (rect.height > 1.25 * rect.width || isRatioOk && randChoice > 5 ) {
            int height2 = rect.height - rect.height / 100 * (randRatio);
            rect.height -= height2;
            int y2 = rect.y + rect.height;
            return new Rectangle[]{new Rectangle(rect.x,rect.y,rect.width,rect.height),new Rectangle(rect.x,y2,rect.width,height2)};
        } else if (rect.width > 1.25 * rect.height ||isRatioOk && randChoice < 5) {
            int width2 = rect.width - rect.width / 100 * (randRatio);
            rect.width -= width2;
            int x2 = rect.x + rect.width;
            return new Rectangle[] {new Rectangle(rect.x,rect.y,rect.width,rect.height),new Rectangle(x2,rect.y,width2,rect.height)};
        }
        return null;
    }

    public int countNodes() {
        if (left == null && right == null) return 1;
        return 1 + left.countNodes() + right.countNodes();
    }

    @Override
    public String toString() {
        return "Container -> x : "+node.x + " | y : "+node.y +" | width : "+node.width + " | height : "+node.height ;
    }

}
