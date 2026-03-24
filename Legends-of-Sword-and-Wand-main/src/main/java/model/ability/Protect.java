package model.ability;

import model.Ability;
import model.Hero;
import model.StatusEffect;
import model.StatusType;

import java.util.List;

/**
 * Order ability — Protect.
 * Casts a shield on ALL friendly party members equal to 10% of each hero's max health.
 * Cost: 25 mana.
 *
 * Hybrid upgrade (Heretic / Fire Shield): handled by subclass FireShield.
 */
public class Protect extends Ability {

    public Protect() {
        super("Protect", 25);
    }

    @Override
    public void execute(Hero caster, List<Hero> targets) {
        for (Hero ally : targets) {
            if (ally.isAlive()) {
                int shieldAmount = (int) (ally.getCurrentMaxHealth() * 0.10);
                // Duration of 1 turn; shield amount tracked directly on Hero via StatusEffect
                ally.addStatusEffect(new StatusEffect(StatusType.SHIELD, 1, shieldAmount));
            }
        }
    }
}
