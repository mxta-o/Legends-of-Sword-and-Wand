package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generated campaign room encounter before it is resolved.
 *
 * For BATTLE rooms, enemies are generated and returned to the presentation
 * layer so combat can be played interactively.
 */
public class CampaignEncounter {

    private final CampaignResult.RoomType roomType;
    private final int roomNumber; // 1-based
    private final List<Hero> enemies;

    public CampaignEncounter(CampaignResult.RoomType roomType, int roomNumber, List<Hero> enemies) {
        this.roomType = roomType;
        this.roomNumber = roomNumber;
        this.enemies = enemies == null ? new ArrayList<>() : new ArrayList<>(enemies);
    }

    public CampaignResult.RoomType getRoomType() {
        return roomType;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public List<Hero> getEnemies() {
        return enemies;
    }

    public boolean isBattle() {
        return roomType == CampaignResult.RoomType.BATTLE;
    }
}
