package model.ability;

import model.Ability;
import model.Hero;

import java.util.List;

/**
 * Mage ability — Replenish.
 * Restores 30 mana to all friendly units and 60 mana to the caster.
 * Cost: 80 mana.
 *
 * Hybrid upgrade (Wizard): cost reduced to 40 mana. Pass {@code true} to wizardMode
 * when constructing for a Wizard hero.
 *
 * Hybrid upgrade (Prophet): friendly spells double their effect —
 * allies receive 60 mana, caster receives 120 mana. Pass {@code true} to doubleEffect.
 *
 * Note: wizardMode and doubleEffect can stack (Wizard-Prophet edge case not in spec,
 * but handled gracefully — doubleEffect is applied on top of the standard amounts).
 */
public class Replenish extends Ability {

    private final boolean doubleEffect;

    /** Standard Mage version. */
    public Replenish() {
        this(false, false);
    }

    /**
     * @param wizardMode   Wizard hybrid: reduces cost from 80 to 40 mana.
     * @param doubleEffect Prophet hybrid: doubles the mana restored to all targets.
     */
    public Replenish(boolean wizardMode, boolean doubleEffect) {
        super("Replenish", wizardMode ? 40 : 80);
        this.doubleEffect = doubleEffect;
    }

    @Override
    public void execute(Hero caster, List<Hero> targets) {
        int alliesAmount = doubleEffect ? 60 : 30;
        int selfAmount   = doubleEffect ? 120 : 60;

        for (Hero ally : targets) {
            if (ally.isAlive()) {
                if (ally == caster) {
                    ally.restoreMana(selfAmount);
                } else {
                    ally.restoreMana(alliesAmount);
                }
            }
        }
        // If caster is not in the targets list (e.g. only enemy list passed), still top up self
        if (!targets.contains(caster)) {
            caster.restoreMana(selfAmount);
        }
    }
}
