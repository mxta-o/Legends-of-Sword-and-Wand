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

    /**
     * Generates (but does not resolve) the next campaign room.
     *
     * This enables interactive UIs to run manual turn-by-turn combat before
     * rewards/penalties are applied.
     */
    public CampaignEncounter createEncounter(Profile profile) {
        if (isCampaignComplete(profile)) {
            throw new IllegalStateException("Campaign is already complete.");
        }

        int roomNumber = profile.getCampaignRoom() + 1;
        boolean isBattle = rollRoomType(profile);

        if (isBattle) {
            return new CampaignEncounter(CampaignResult.RoomType.BATTLE, roomNumber, generateEnemyParty(profile));
        }
        return new CampaignEncounter(CampaignResult.RoomType.INN, roomNumber, List.of());
    }

    /**
     * Resolves a previously generated encounter and advances campaign progress.
     *
     * @param profile   active profile
     * @param encounter generated encounter from {@link #createEncounter(Profile)}
     * @param battleWon whether the player won the manual battle (ignored for INN)
     */
    public CampaignResult resolveEncounter(Profile profile, CampaignEncounter encounter, boolean battleWon) {
        if (encounter == null) {
            throw new IllegalArgumentException("Encounter cannot be null.");
        }
        if (isCampaignComplete(profile)) {
            throw new IllegalStateException("Campaign is already complete.");
        }

        int expectedRoom = profile.getCampaignRoom() + 1;
        if (encounter.getRoomNumber() != expectedRoom) {
            throw new IllegalArgumentException("Encounter room does not match current campaign progress.");
        }

        CampaignResult result;
        if (encounter.getRoomType() == CampaignResult.RoomType.BATTLE) {
            result = resolveBattleOutcome(profile, expectedRoom, encounter.getEnemies(), battleWon);
        } else {
            result = runInnRoom(profile, expectedRoom);
        }

        profile.advanceCampaignRoom();
        if (isCampaignComplete(profile)) {
            int score = calculateFinalScore(profile);
            profile.endCampaign(score);
        }
        return result;
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
        battleService.startBattle(profile.getActiveParty(), enemies);
        boolean playerWon = isPlayerPartyAlive(profile) && enemies.stream().noneMatch(Hero::isAlive);
        return resolveBattleOutcome(profile, roomNumber, enemies, playerWon);
    }

    private CampaignResult resolveBattleOutcome(Profile profile, int roomNumber,
                                                List<Hero> enemies, boolean playerWon) {
        boolean finalWin = playerWon && isPlayerPartyAlive(profile);
        if (finalWin) {
            return applyVictoryRewards(profile, roomNumber, enemies);
        }

        return applyDefeatPenalty(profile, roomNumber);
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
        // 1–3 enemies per room for demo purposes
        int partySize    = 1 + random.nextInt(2);
        int cumLevel     = Math.max(1, profile.getCumulativePartyLevel());
        int minCumLevel  = Math.max(1, cumLevel - 10);
        int targetCumLvl = minCumLevel + random.nextInt(Math.max(1, cumLevel - minCumLevel + 1));

        List<Hero> enemies = new ArrayList<>();

        // Distribute targetCumLvl across partySize enemies (1..10 each)
        int[] levels = distributeLevels(targetCumLvl, partySize);

        for (int i = 0; i < partySize; i++) {
            int level = Math.max(1, Math.min(10, levels[i]));
            enemies.add(buildEnemy(level, i + 1));
        }
        normalizeEnemyHealth(enemies);

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

    private CampaignResult applyVictoryRewards(Profile profile, int roomNumber, List<Hero> enemies) {
        int totalExp = 0;
        int totalGold = 0;
        for (Hero enemy : enemies) {
            totalExp += 50 * enemy.getLevel();
            totalGold += 75 * enemy.getLevel();
        }

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
    }

    private CampaignResult applyDefeatPenalty(Profile profile, int roomNumber) {
        profile.applyGoldPenalty();
        applyExpPenalty(profile.getActiveParty());
        innService.visitInn(profile);
        return new CampaignResult(CampaignResult.RoomType.BATTLE, roomNumber,
                false, 0, 0, List.of());
    }

    private Hero buildEnemy(int level, int index) {
        HeroClass enemyClass = pickEnemyClass();
        Hero enemy = new Hero("Goblin-" + index, enemyClass);
        for (int lvl = 1; lvl < level; lvl++) {
            enemy.levelUp(enemyClass);
        }
        return enemy;
    }

    private HeroClass pickEnemyClass() {
        double r = random.nextDouble();
        if (r < 0.45) return HeroClass.CHAOS;
        if (r < 0.75) return HeroClass.MAGE;
        if (r < 0.95) return HeroClass.ORDER;
        return HeroClass.WARRIOR;
    }

    private void normalizeEnemyHealth(List<Hero> enemies) {
        for (Hero enemy : enemies) {
            if (enemy.getLevel() <= 10) {
                int desiredHp = 1;
                int delta = desiredHp - enemy.getCurrentMaxHealth();
                if (delta != 0) {
                    enemy.addMaxHealth(delta);
                }
                enemy.revive();
            }
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
