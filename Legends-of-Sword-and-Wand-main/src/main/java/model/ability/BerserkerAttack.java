package model.ability;

import model.Ability;
import model.Hero;
import model.StatusEffect;
import model.StatusType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Warrior ability — Berserker Attack.
 * Attacks the primary target for full damage, then damages 2 MORE units for 25%
 * of the original damage.
 * Cost: 60 mana.
 *
 * Hybrid upgrades:
 *   - Knight (Warrior+Warrior spec): 50% chance to STUN the splash targets.
 *   - Paladin (Order+Warrior hybrid): heals the caster for 10% of their max HP
 *     BEFORE launching the attack.
 *
 * Controlled by the {@code stunSplash} and {@code healBeforeAttack} flags.
 */
public class BerserkerAttack extends Ability {

    private final boolean stunSplash;
    private final boolean healBeforeAttack;
    private final Random random;

    /** Standard Warrior version. */
    public BerserkerAttack() {
        this(false, false);
    }

    /**
     * @param stunSplash      Knight hybrid: splash targets have 50% chance to be stunned.
     * @param healBeforeAttack Paladin hybrid: heals caster for 10% max HP before attacking.
     */
    public BerserkerAttack(boolean stunSplash, boolean healBeforeAttack) {
        super("Berserker Attack", 60);
        this.stunSplash = stunSplash;
        this.healBeforeAttack = healBeforeAttack;
        this.random = new Random();
    }

    @Override
    public void execute(Hero caster, List<Hero> targets) {
        if (targets.isEmpty()) return;

        // Paladin pre-attack heal
        if (healBeforeAttack) {
            int healAmount = (int) (caster.getCurrentMaxHealth() * 0.10);
            caster.heal(healAmount);
        }

        // Filter alive targets
        List<Hero> alive = new ArrayList<>();
        for (Hero t : targets) {
            if (t.isAlive()) alive.add(t);
        }
        if (alive.isEmpty()) return;

        // Primary target: full damage
        Hero primary = alive.get(0);
        int primaryDamage = Math.max(0, caster.getCurrentAttack() - primary.getCurrentDefense());
        primary.takeDamage(primaryDamage);

        // Splash: up to 2 additional targets for 25% of primary damage
        int splashDamage = (int) (primaryDamage * 0.25);
        int splashCount = 0;
        for (int i = 1; i < alive.size() && splashCount < 2; i++) {
            Hero splashTarget = alive.get(i);
            splashTarget.takeDamage(splashDamage);
            // Knight: 50% chance to stun each splash target
            if (stunSplash && random.nextDouble() < 0.50) {
                splashTarget.addStatusEffect(new StatusEffect(StatusType.STUN, 1));
            }
            splashCount++;
        }
    }
}
