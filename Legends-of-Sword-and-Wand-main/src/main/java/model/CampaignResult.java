package model;

import java.util.List;

/**
 * Captures the outcome of one campaign room encounter.
 */
public class CampaignResult {

    public enum RoomType { BATTLE, INN }

    private final RoomType roomType;
    private final int roomNumber;      // 1-based
    private final boolean battleWon;   // only meaningful if roomType == BATTLE
    private final int expGained;
    private final int goldGained;
    private final List<Hero> survivingHeroes;

    public CampaignResult(RoomType roomType, int roomNumber,
                          boolean battleWon, int expGained, int goldGained,
                          List<Hero> survivingHeroes) {
        this.roomType       = roomType;
        this.roomNumber     = roomNumber;
        this.battleWon      = battleWon;
        this.expGained      = expGained;
        this.goldGained     = goldGained;
        this.survivingHeroes = survivingHeroes;
    }

    public RoomType      getRoomType()       { return roomType; }
    public int           getRoomNumber()     { return roomNumber; }
    public boolean       isBattleWon()       { return battleWon; }
    public int           getExpGained()      { return expGained; }
    public int           getGoldGained()     { return goldGained; }
    public List<Hero>    getSurvivingHeroes(){ return survivingHeroes; }

    @Override
    public String toString() {
        if (roomType == RoomType.INN) {
            return String.format("[Room %d] Inn visit — party revived and restocked.", roomNumber);
        }
        if (battleWon) {
            return String.format("[Room %d] Battle WON — +%d exp, +%dg", roomNumber, expGained, goldGained);
        }
        return String.format("[Room %d] Battle LOST — penalties applied.", roomNumber);
    }
}
