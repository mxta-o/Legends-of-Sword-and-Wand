package model.ability;

import model.Ability;
import model.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chaos ability — Fireball.
 * Deals damage (caster attack - target defense) to AT MOST 3 enemy units.
 * The first target in the list is the primary target; up to 2 additional targets
 * are hit automatically.
 * Cost: 30 mana.
 *
 * Hybrid upgrade (Sorcerer): double damage to all affected units.
 * Controlled by the {@code doubleDamage} flag set when the hero becomes a Sorcerer.
 */
public class Fireball extends Ability {

    private static final int MAX_TARGETS = 3;
    private final boolean doubleDamage;

    /** Standard Chaos version. */
    public Fireball() {
        this(false);
    }

    /** Sorcerer hybrid version — pass {@code true} to double all damage. */
    public Fireball(boolean doubleDamage) {
        super("Fireball", 30);
        this.doubleDamage = doubleDamage;
    }

    @Override
    public void execute(Hero caster, List<Hero> targets) {
        List<Hero> alive = new ArrayList<>();
        for (Hero t : targets) {
            if (t.isAlive()) alive.add(t);
        }
        if (alive.isEmpty()) return;

        // Hit up to MAX_TARGETS enemies; first target is the chosen primary
        int hits = Math.min(MAX_TARGETS, alive.size());
        for (int i = 0; i < hits; i++) {
            Hero target = alive.get(i);
            int rawDamage = Math.max(0, caster.getCurrentAttack() - target.getCurrentDefense());
            int damage = doubleDamage ? rawDamage * 2 : rawDamage;
            target.takeDamage(damage);
        }
    }
}
