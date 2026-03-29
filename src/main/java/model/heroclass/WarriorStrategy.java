package model.heroclass;

import model.Ability;
import model.Hero;
import model.HeroClassStrategy;
import model.HeroClass;
import model.ability.BerserkerAttack;

import java.util.Arrays;
import java.util.List;

/**
 * Warrior class strategy.
 * Per-level bonus: +2 attack, +3 defense.
 * Abilities: Berserker Attack (60 mana).
 * With the use of AI
 */
public class WarriorStrategy implements HeroClassStrategy {

    @Override
    public void applyLevelBonus(Hero hero) {
        hero.addBaseAttack(2);
        hero.addBaseDefense(3);
    }

    @Override
    public List<Ability> getAbilities(Hero hero) {
        // Default flags
        boolean stunSplash = false;
        boolean healBefore = false;

        // If hero has specialized as Warrior (reached level 5) and is not hybrid,
        // treat as Knight: Berserker gains 50% stun on splash targets.
        if (hero.getSpecializationClass() == HeroClass.WARRIOR && hero.getHybridClass() == null && hero.getClassLevel(HeroClass.WARRIOR) >= 5) {
            stunSplash = true;
        }
        // Paladin: Order+Warrior hybrid heals before attack
        if (hero.getHybridClass() == HeroClass.ORDER && hero.getClassLevel(HeroClass.WARRIOR) >= 5) {
            healBefore = true;
        }

        return Arrays.asList(new BerserkerAttack(stunSplash, healBefore));
    }

    @Override
    public String getClassName() {
        return "Warrior";
    }
}
