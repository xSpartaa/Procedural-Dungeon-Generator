package dungeon;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Données complètes du donjon généré.
 *
 * Accès simple pour le RPG :
 *   rooms[]     → toutes les salles avec leurs IDs
 *   corridors[] → tous les couloirs
 *   entrance    → salle d'entrée
 *   boss        → salle du boss
 *   miniBoss    → salle du mini-boss
 */
public class DungeonData {

    public final Room[]     rooms;
    public final Corridor[] corridors;
    public final Room       entrance;
    public final Room       boss;
    public final Room       miniBoss;

    DungeonData(Room[] rooms, Corridor[] corridors,
                Room entrance, Room boss, Room miniBoss) {
        this.rooms     = rooms;
        this.corridors = corridors;
        this.entrance  = entrance;
        this.boss      = boss;
        this.miniBoss  = miniBoss;
    }

    /** Retourne la salle par ID, ou null. */
    public Room getRoom(int id) {
        for (Room r : rooms) if (r.id == id) return r;
        return null;
    }

    /** Retourne toutes les salles voisines d'une salle donnée. */
    public List<Room> neighbors(Room room) {
        List<Room> result = new ArrayList<>();
        for (Corridor c : corridors) {
            if (c.roomA == room) result.add(c.roomB);
            else if (c.roomB == room) result.add(c.roomA);
        }
        return result;
    }
}