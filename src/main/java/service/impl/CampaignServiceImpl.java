package service.impl;

import model.*;
import service.BattleService;
import service.CampaignService;
import service.InnService;

import java.util.*;

/**
 * Concrete implementation of CampaignService (M3 — PvE Campaign, UC6).
 *
 * Encounter probability formula (from spec):
 *   battleChance = min(0.90, 0.60 + 0.03 * floor(cumulativeLevel / 10))
 *   innChance    = 1 - battleChance
 *
 * Enemy party generation:
 *   - 1–5 enemy units
 *   - Their cumulative level is drawn in the range
 *     [max(1, partyCumLevel - 10), partyCumLevel]
 *   - Each enemy's attack, defense and HP are scaled from the base stats
 *     at a rate of +1 atk/def per level and +10 HP per level.
 *
 * Experience and gold (per spec):
 *   Exp per enemy = 50 * enemyLevel    (split among surviving heroes)
 *   Gold per enemy = 75 * enemyLevel   (added to profile gold)
 *   On loss: -10% gold, -30% experience progress for each hero
 *
 * Final score:
 *   (Σ hero levels × 100) + (gold × 10) + (Σ purchasedItems × itemCost / 2 × 10)
 */
public class CampaignServiceImpl implements CampaignService {

    private static final int TOTAL_ROOMS        = 30;
    private static final double BASE_BATTLE_PCT = 0.60;
    private static final double INN_SHIFT_RATE  = 0.03;   // per 10 cumulative levels
    private static final double MAX_BATTLE_PCT  = 0.90;
    private static final double EXP_PENALTY     = 0.30;

    private final BattleService battleService;
    private final InnService    innService;
    private final Random        random;

    // Tracks items purchased during the campaign for scoring purposes
    // Key: profile player name → total item-value spent
    private final Map<String, Integer> itemValueSpent = new HashMap<>();

    public CampaignServiceImpl(BattleService battleService, InnService innService) {
        this.battleService = battleService;
        this.innService    = innService;
        this.random        = new Random();
    }

    /** Package-private for deterministic unit testing. */
    public CampaignServiceImpl(BattleService battleService, InnService innService, Random random) {
        this.battleService = battleService;
        this.innService    = innService;
        this.random       = random;
    }

    // -------------------------------------------------------------------------
    // CampaignService interface
    // -------------------------------------------------------------------------

    @Override
    public void startCampaign(Profile profile) {
        profile.startCampaign();
        itemValueSpent.put(profile.getPlayerName(), 0);
    }

    @Override
    public CampaignResult enterNextRoom(Profile profile) {
        if (isCampaignComplete(profile)) {
            throw new IllegalStateException("Campaign is already complete.");
        }

        int roomNumber = profile.getCampaignRoom() + 1; // 1-based for display
        boolean isBattle = rollRoomType(profile);

        CampaignResult result;
        if (isBattle) {
            result = runBattleRoom(profile, roomNumber);
        } else {
            result = runInnRoom(profile, roomNumber);
        }

        profile.advanceCampaignRoom();

        if (isCampaignComplete(profile)) {
            int score = calculateFinalScore(profile);
            profile.endCampaign(score);
        }

        return result;
    }

    @Override
    public int calculateFinalScore(Profile profile) {
        int heroLevelScore = profile.getActiveParty().stream()
                .mapToInt(Hero::getLevel).sum() * 100;

        int goldScore = profile.getGold() * 10;

        int itemScore = itemValueSpent.getOrDefault(profile.getPlayerName(), 0) * 10;

        return heroLevelScore + goldScore + itemScore;
    }

    @Override
    public boolean isCampaignComplete(Profile profile) {
        return profile.getCampaignRoom() >= TOTAL_ROOMS;
    }

    // -------------------------------------------------------------------------
    // Room logic
    // -------------------------------------------------------------------------

    /** Returns true for BATTLE, false for INN. */
    private boolean rollRoomType(Profile profile) {
        int cumLevel  = profile.getCumulativePartyLevel();
        double battle = Math.min(MAX_BATTLE_PCT,
                BASE_BATTLE_PCT + INN_SHIFT_RATE * Math.floor(cumLevel / 10.0));
        return random.nextDouble() < battle;
    }

    private CampaignResult runBattleRoom(Profile profile, int roomNumber) {
        List<Hero> enemies = generateEnemyParty(profile);
        BattleResult battleResult = battleService.startBattle(profile.getActiveParty(), enemies);

        if (!battleResult.isDraw() && battleResult.getWinningTeam() == profile.getActiveParty()
                || isPlayerPartyAlive(profile)) {
            // Player won
            int totalExp  = 0;
            int totalGold = 0;
            for (Hero enemy : enemies) {
                totalExp  += 50 * enemy.getLevel();
                totalGold += 75 * enemy.getLevel();
            }

            // Distribute exp only to surviving heroes
            List<Hero> survivors = profile.getActiveParty().stream()
                    .filter(Hero::isAlive)
                    .collect(java.util.stream.Collectors.toList());

            if (!survivors.isEmpty()) {
                int expPerHero = totalExp / survivors.size();
                survivors.forEach(h -> h.gainExperience(expPerHero));
            }

            profile.addGold(totalGold);

            return new CampaignResult(CampaignResult.RoomType.BATTLE, roomNumber,
                    true, totalExp, totalGold, survivors);

        } else {
            // Player lost — apply penalties
            profile.applyGoldPenalty();
            applyExpPenalty(profile.getActiveParty());

            // Revive the party at the last inn (represented by a free inn visit)
            innService.visitInn(profile);

            return new CampaignResult(CampaignResult.RoomType.BATTLE, roomNumber,
                    false, 0, 0, List.of());
        }
    }

    private CampaignResult runInnRoom(Profile profile, int roomNumber) {
        innService.visitInn(profile);

        return new CampaignResult(CampaignResult.RoomType.INN, roomNumber,
                false, 0, 0, profile.getActiveParty());
    }

    // -------------------------------------------------------------------------
    // Enemy generation
    // -------------------------------------------------------------------------

    /**
     * Generates a random enemy party scaled to the player's cumulative level.
     * Enemies have no special abilities — they can only ATTACK, DEFEND, or WAIT.
     */
    private List<Hero> generateEnemyParty(Profile profile) {
        int partySize    = 1 + random.nextInt(5);      // 1-5 enemies
        int cumLevel     = Math.max(1, profile.getCumulativePartyLevel());
        int minCumLevel  = Math.max(1, cumLevel - 10);
        int targetCumLvl = minCumLevel + random.nextInt(Math.max(1, cumLevel - minCumLevel + 1));

        List<Hero> enemies = new ArrayList<>();

        // Distribute targetCumLvl across partySize enemies (1..10 each)
        int[] levels = distributeLevels(targetCumLvl, partySize);

        for (int i = 0; i < partySize; i++) {
            int level = Math.max(1, Math.min(10, levels[i]));
            Hero enemy = new Hero("Enemy-" + (i + 1), HeroClass.WARRIOR);
            // Level up the enemy (base-class only, no specials)
            for (int lvl = 1; lvl < level; lvl++) {
                enemy.levelUp(HeroClass.WARRIOR);
            }
            enemies.add(enemy);
        }

        return enemies;
    }

    /** Distributes totalLevels across n slots, each at least 1. */
    private int[] distributeLevels(int totalLevels, int n) {
        int[] levels = new int[n];
        Arrays.fill(levels, 1);
        int remaining = totalLevels - n;
        for (int i = 0; i < n && remaining > 0; i++) {
            int add = (i == n - 1) ? remaining : random.nextInt(remaining + 1);
            levels[i] += add;
            remaining  -= add;
        }
        return levels;
    }

    // -------------------------------------------------------------------------
    // Penalties
    // -------------------------------------------------------------------------

    private void applyExpPenalty(List<Hero> heroes) {
        for (Hero hero : heroes) {
            hero.applyExperiencePenalty(EXP_PENALTY);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Checks if at least one hero in the player's party is alive. */
    private boolean isPlayerPartyAlive(Profile profile) {
        return profile.getActiveParty().stream().anyMatch(Hero::isAlive);
    }

    /**
     * Tracks item-value purchases for the final score.
     * Should be called by the InnService adapter layer or the controller
     * whenever the player buys an item.
     */
    public void recordItemPurchase(Profile profile, InnItem item) {
        String key    = profile.getPlayerName();
        int previous  = itemValueSpent.getOrDefault(key, 0);
        // Score contribution = itemCost / 2 (per spec "half the price ... times 10")
        itemValueSpent.put(key, previous + item.getCost() / 2);
    }
}
