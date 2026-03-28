package model;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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
    private String passwordHash; // hex
    private String passwordSalt; // hex
    // Simple inventory: counts of purchased InnItems
    private Map<InnItem, Integer> inventory;

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
        this.passwordHash   = null;
        this.passwordSalt   = null;
        this.inventory = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Inventory helpers
    // -------------------------------------------------------------------------

    public int getInventoryCount(InnItem item) {
        return inventory.getOrDefault(item, 0);
    }

    public void addInventoryItem(InnItem item) {
        inventory.put(item, getInventoryCount(item) + 1);
    }

    public boolean removeInventoryItem(InnItem item) {
        int c = getInventoryCount(item);
        if (c <= 0) return false;
        if (c == 1) inventory.remove(item);
        else inventory.put(item, c - 1);
        return true;
    }

    /**
     * Sets the account password using PBKDF2WithHmacSHA256 with a random salt.
     * Pass null/empty to clear the password.
     */
    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            this.passwordHash = null;
            this.passwordSalt = null;
            return;
        }
        try {
            byte[] salt = new byte[16];
            java.security.SecureRandom rnd = new java.security.SecureRandom();
            rnd.nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, 65536, 256);
            this.passwordSalt = toHex(salt);
            this.passwordHash = toHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set password", e);
        }
    }

    /**
     * Verifies a plaintext password against the stored salt+hash.
     */
    public boolean verifyPassword(String password) {
        if (this.passwordHash == null || this.passwordSalt == null) {
            return password == null || password.isEmpty();
        }
        if (password == null) return false;
        try {
            byte[] salt = fromHex(this.passwordSalt);
            byte[] expected = fromHex(this.passwordHash);
            byte[] actual = pbkdf2(password.toCharArray(), salt, 65536, 256);
            return java.util.Arrays.equals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws Exception {
        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password, salt, iterations, keyLength);
        javax.crypto.SecretKey key = skf.generateSecret(spec);
        return key.getEncoded();
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        return out;
    }

    /**
     * Package-access helper used by repository to restore stored hash/salt.
     */
    public void setPasswordHashAndSalt(String hashHex, String saltHex) {
        this.passwordHash = hashHex;
        this.passwordSalt = saltHex;
    }

    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }

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
        // Save a deep-copy snapshot of the heroes so saved parties do not
        // share mutable Hero instances with active party or other saved slots.
        List<Hero> copy = new ArrayList<>();
        for (Hero h : party) {
            if (h != null) copy.add(h.copy());
        }
        savedParties.add(copy);
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
        // Load as deep copies so active party modifications don't mutate saved snapshots.
        this.activeParty = new ArrayList<>();
        for (Hero h : party) {
            if (h != null) this.activeParty.add(h.copy());
        }
    }

    public boolean addHeroToParty(Hero hero) {
        if (activeParty.size() >= 5) return false;
        if (hero == null) return false;
        // Prevent accidental duplicate inserts of the same hero (same name, class, level)
        for (Hero h : activeParty) {
            if (h != null && h.getName().equals(hero.getName())
                    && h.getHeroClass() == hero.getHeroClass()
                    && h.getLevel() == hero.getLevel()) {
                return false;
            }
        }
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

    /**
     * Pause/exit the currently active campaign without finalizing the score.
     * The progress (current room) is kept so the campaign can be resumed later.
     */
    public void pauseCampaign() {
        this.campaignActive = false;
    }

    /**
     * Resume a previously-paused campaign without resetting progress.
     */
    public void resumeCampaign() {
        this.campaignActive = true;
    }

    @Override
    public String toString() {
        return String.format("Profile{player='%s', gold=%d, pvp=%d/%d, room=%d}",
                playerName, gold, pvpWins, pvpLosses, campaignRoom);
    }
}
