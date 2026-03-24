package model.ability;

import model.Ability;
import model.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chaos ability — Chain Lightning.
 * Hits ALL enemy units in a random order starting from the chosen first target.
 * First unit takes 100% of attack damage; every subsequent unit takes 25% of the
 * damage dealt to the previous unit (i.e. it halves-ish each step: 100 → 25 → 6 → ...).
 * Cost: 40 mana.
 *
 * Hybrid upgrade (Invoker): each subsequent target takes 50% of the previous damage
 * instead of 25%. Controlled by the {@code improvedChain} flag.
 */
public class ChainLightning extends Ability {

    private final boolean improvedChain;

    /** Standard Chaos version (25% carry-over). */
    public ChainLightning() {
        this(false);
    }

    /** Invoker hybrid version — pass {@code true} for 50% carry-over. */
    public ChainLightning(boolean improvedChain) {
        super("Chain Lightning", 40);
        this.improvedChain = improvedChain;
    }

    @Override
    public void execute(Hero caster, List<Hero> targets) {
        if (targets.isEmpty()) return;

        // Build the hit order: first target is the chosen one, rest are shuffled
        List<Hero> alive = new ArrayList<>();
        for (Hero t : targets) {
            if (t.isAlive()) alive.add(t);
        }
        if (alive.isEmpty()) return;

        Hero primaryTarget = alive.get(0);
        List<Hero> rest = new ArrayList<>(alive.subList(1, alive.size()));
        Collections.shuffle(rest);

        List<Hero> hitOrder = new ArrayList<>();
        hitOrder.add(primaryTarget);
        hitOrder.addAll(rest);

        double carryOver = improvedChain ? 0.50 : 0.25;
        int baseDamage = Math.max(0, caster.getCurrentAttack() - primaryTarget.getCurrentDefense());
        double currentDamage = baseDamage;

        for (Hero target : hitOrder) {
            int damage = (int) currentDamage;
            target.takeDamage(damage);
            currentDamage *= carryOver;
            if (currentDamage < 1) break; // No point continuing below 1 damage
        }
    }
}
