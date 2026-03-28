package service.impl;

import model.Hero;
import model.HeroClass;
import model.InnItem;
import model.Profile;
import service.InnService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Concrete implementation of InnService (UC7 — Inn Visit).
 *
 * On arrival:
 *   - All heroes are automatically revived and fully restored (free).
 * Shop:
 *   - Players may purchase food (+HP) or drink (+mana) items.
 * Recruitment:
 *   - In the first 10 rooms, 1–3 unemployed heroes of random class (level 1–4) appear.
 *   - Level 1: free; level 2+: 200g per level.
 */
public class InnServiceImpl implements InnService {

    private static final int MAX_PARTY_SIZE       = 5;
    private static final int RECRUIT_ROOM_LIMIT   = 10;
    private static final int RECRUIT_COST_PER_LVL = 200;
    private static final int MAX_RECRUIT_LEVEL    = 4;

    private final Random random;

    public InnServiceImpl() {
        this.random = new Random();
    }

    /** Package-private constructor for deterministic testing. */
    InnServiceImpl(Random random) {
        this.random = random;
    }

    // -------------------------------------------------------------------------
    // InnService interface
    // -------------------------------------------------------------------------

    @Override
    public void visitInn(Profile profile) {
        for (Hero hero : profile.getActiveParty()) {
            hero.revive(); // revive() sets isAlive=true, full HP and full mana
        }
    }

    @Override
    public boolean purchaseItem(Profile profile, InnItem item, Hero targetHero) {
        if (!profile.spendGold(item.getCost())) {
            return false; // insufficient funds
        }
        // If no target is provided, interpret as purchase-only (added to inventory in future).
        if (targetHero != null) {
            item.applyTo(targetHero);
        }
        return true;
    }

    @Override
    public List<Hero> getRecruitableHeroes(Profile profile, int roomNumber) {
        List<Hero> candidates = new ArrayList<>();

        // Heroes only appear in rooms 1-10 and only when party has room
        if (roomNumber > RECRUIT_ROOM_LIMIT
                || profile.getActiveParty().size() >= MAX_PARTY_SIZE) {
            return candidates;
        }

        // Generate 1-3 candidate heroes
        int count = 1 + random.nextInt(3);
        HeroClass[] classes = { HeroClass.ORDER, HeroClass.CHAOS, HeroClass.WARRIOR, HeroClass.MAGE };

        for (int i = 0; i < count; i++) {
            HeroClass randClass = classes[random.nextInt(classes.length)];
            int randLevel       = 1 + random.nextInt(MAX_RECRUIT_LEVEL); // 1..4

            Hero candidate = new Hero("Wanderer-" + (i + 1), randClass);

            // Apply level-ups so the hero has the proper stats
            for (int lvl = 1; lvl < randLevel; lvl++) {
                candidate.levelUp(randClass);
            }

            candidates.add(candidate);
        }

        return candidates;
    }

    @Override
    public boolean recruitHero(Profile profile, Hero candidate) {
        // Party full
        if (profile.getActiveParty().size() >= MAX_PARTY_SIZE) return false;

        int level = candidate.getLevel();
        int cost  = (level == 1) ? 0 : level * RECRUIT_COST_PER_LVL;

        if (!profile.spendGold(cost)) return false;

        boolean added = profile.addHeroToParty(candidate);
        if (!added) return false;
        return true;
    }

    // -------------------------------------------------------------------------
    // Recruitment cost helper (public for UI display)
    // -------------------------------------------------------------------------

    /**
     * Returns how much it costs to recruit a hero of the given level.
     * Level 1 is always free.
     */
    public static int recruitmentCost(int heroLevel) {
        return (heroLevel <= 1) ? 0 : heroLevel * RECRUIT_COST_PER_LVL;
    }
}
