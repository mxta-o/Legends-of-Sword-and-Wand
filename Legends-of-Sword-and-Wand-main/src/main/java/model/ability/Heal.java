package model.ability;

import model.Ability;
import model.Hero;

import java.util.List;

/**
 * Order ability — Heal.
 * Heals the ally with the LOWEST current health for 25% of that ally's max health.
 * Cost: 35 mana.
 *
 * Hybrid upgrade (Priest): heals ALL friendly units instead of just the lowest.
 * Controlled by the {@code healAll} flag set when the hero becomes a Priest.
 */
public class Heal extends Ability {

    private final boolean healAll;

    /** Standard Order version — heals the lowest HP ally. */
    public Heal() {
        this(false);
    }

    /** Priest hybrid version — pass {@code true} to heal all allies. */
    public Heal(boolean healAll) {
        super("Heal", 35);
        this.healAll = healAll;
    }

    @Override
    public void execute(Hero caster, List<Hero> targets) {
        if (targets.isEmpty()) return;

        if (healAll) {
            for (Hero ally : targets) {
                if (ally.isAlive()) {
                    int healAmount = (int) (ally.getCurrentMaxHealth() * 0.25);
                    ally.heal(healAmount);
                }
            }
        } else {
            Hero lowestHp = null;
            for (Hero ally : targets) {
                if (!ally.isAlive()) continue;
                if (lowestHp == null || ally.getCurrentHealth() < lowestHp.getCurrentHealth()) {
                    lowestHp = ally;
                }
            }
            if (lowestHp != null) {
                int healAmount = (int) (lowestHp.getCurrentMaxHealth() * 0.25);
                lowestHp.heal(healAmount);
            }
        }
    }
}
