package dungeon;

import java.util.concurrent.ThreadLocalRandom;
import java.awt.*;

public class Room {
    static int min_room_size = 10;

    int x;
    int y;
    int width;
    int height;

    public Room(Rectangle rect) {
        this.width = getRandomInRange(min_room_size, rect.width - 2);
        this.height = getRandomInRange(min_room_size, rect.height - 2);
        this.x = rect.x + getRandomInRange(1, rect.width - this.width - 1);
        this.y = rect.y + getRandomInRange(1, rect.height - this.height - 1);
    }

    public int getRandomInRange(int min, int max) {
        if (min >= max) return min; // Sécurité si l'espace est trop petit
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}


