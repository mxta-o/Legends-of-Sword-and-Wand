package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player profile in Legends of Sword and Wand.
 *
 * A profile stores the player's name, their gold, up to 5 saved parties,
 * PvP stats (wins/losses), and the campaign progress for an active run.
 */
public class Profile {

    private String playerName;
    private int gold;
    private List<List<Hero>> savedParties;   // up to 5 saved parties
    private int pvpWins;
    private int pvpLosses;

    // Active campaign state
    private List<Hero> activeParty;
    private int campaignRoom;          // 0-based room index (0..29)
    private boolean campaignActive;
    private int campaignScore;

    // Hall-of-fame / historical best
    private int highScore;

    public Profile(String playerName) {
        this.playerName   = playerName;
        this.gold         = 0;
        this.savedParties = new ArrayList<>();
        this.pvpWins      = 0;
        this.pvpLosses    = 0;
        this.activeParty  = new ArrayList<>();
        this.campaignRoom = 0;
        this.campaignActive = false;
        this.campaignScore  = 0;
        this.highScore      = 0;
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    public String getPlayerName() { return playerName; }
    public void   setPlayerName(String name) { this.playerName = name; }

    // -------------------------------------------------------------------------
    // Gold
    // -------------------------------------------------------------------------

    public int getGold() { return gold; }

    public void addGold(int amount) {
        if (amount > 0) gold += amount;
    }

    /**
     * Deducts gold. Returns false and makes no change if funds are insufficient.
     */
    public boolean spendGold(int amount) {
        if (amount < 0 || gold < amount) return false;
        gold -= amount;
        return true;
    }

    /**
     * Lose 10% of gold on battle defeat (rounded down).
     */
    public void applyGoldPenalty() {
        gold -= gold / 10;
    }

    // -------------------------------------------------------------------------
    // Saved parties (max 5)
    // -------------------------------------------------------------------------

    public List<List<Hero>> getSavedParties() { return savedParties; }

    /**
     * Saves a deep-copy snapshot of the party. Returns false if the limit is reached.
     */
    public boolean saveParty(List<Hero> party) {
        if (savedParties.size() >= 5) return false;
        savedParties.add(new ArrayList<>(party));
        return true;
    }

    public List<Hero> getSavedParty(int index) {
        if (index < 0 || index >= savedParties.size()) return null;
        return savedParties.get(index);
    }

    // -------------------------------------------------------------------------
    // PvP stats
    // -------------------------------------------------------------------------

    public int getPvpWins()   { return pvpWins; }
    public int getPvpLosses() { return pvpLosses; }
    public void addPvpWin()   { pvpWins++; }
    public void addPvpLoss()  { pvpLosses++; }

    // -------------------------------------------------------------------------
    // Active party
    // -------------------------------------------------------------------------

    public List<Hero> getActiveParty() { return activeParty; }

    public void setActiveParty(List<Hero> party) {
        this.activeParty = new ArrayList<>(party);
    }

    public boolean addHeroToParty(Hero hero) {
        if (activeParty.size() >= 5) return false;
        activeParty.add(hero);
        return true;
    }

    public int getCumulativePartyLevel() {
        return activeParty.stream().mapToInt(Hero::getLevel).sum();
    }

    // -------------------------------------------------------------------------
    // Campaign progress
    // -------------------------------------------------------------------------

    public int  getCampaignRoom()    { return campaignRoom; }
    public boolean isCampaignActive() { return campaignActive; }
    public int  getCampaignScore()   { return campaignScore; }
    public int  getHighScore()       { return highScore; }

    public void startCampaign() {
        campaignRoom   = 0;
        campaignActive = true;
        campaignScore  = 0;
    }

    public void advanceCampaignRoom() {
        campaignRoom++;
        if (campaignRoom >= 30) {
            campaignActive = false;
        }
    }

    public void setCampaignScore(int score) {
        this.campaignScore = score;
        if (score > highScore) highScore = score;
    }

    public void endCampaign(int finalScore) {
        campaignActive = false;
        setCampaignScore(finalScore);
    }

    @Override
    public String toString() {
        return String.format("Profile{player='%s', gold=%d, pvp=%d/%d, room=%d}",
                playerName, gold, pvpWins, pvpLosses, campaignRoom);
    }
}
