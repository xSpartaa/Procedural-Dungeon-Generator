package dungeon;

import java.awt.*;

public class Node {
    Rectangle node;
    Room room;
    public Node left;
    public Node right;

    public Node(Rectangle node) {
        this.node = node;
        generate(15);

        //Si le nœud est une feuille, créer la Room.
        if (left == null && right == null) {
            room = new Room(this.node);
        }
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
        // 1. On décide de l'orientation du split (Horizontal ou Vertical)
        boolean splitHorizontal;
        double ratio = (double) rect.width / rect.height;

        if (rect.height > rect.width && 1.0 / ratio > 1.25) {
            splitHorizontal = true; // Trop haut → coupe horizontale obligatoire
        } else if (rect.width > rect.height && ratio > 1.25) {
            splitHorizontal = false; // Trop large → coupe verticale obligatoire
        } else {
            splitHorizontal = Math.random() < 0.5; // Équilibré → hasard 50/50
        }

        // 2. On calcule la proportion de la découpe (entre 30% et 70%)
        double randPercent = 0.3 + (Math.random() * 0.4);

        // 3. On crée les deux nouveaux rectangles SANS modifier l'original 'rect'
        if (splitHorizontal) {
            int h1 = (int) (rect.height * randPercent);
            int h2 = rect.height - h1; // Le reste va au deuxième enfant

            return new Rectangle[] {
                    new Rectangle(rect.x, rect.y, rect.width, h1),
                    new Rectangle(rect.x, rect.y + h1, rect.width, h2)
            };
        } else {
            int w1 = (int) (rect.width * randPercent);
            int w2 = rect.width - w1;

            return new Rectangle[] {
                    new Rectangle(rect.x, rect.y, w1, rect.height),
                    new Rectangle(rect.x + w1, rect.y, w2, rect.height)
            };
        }
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
