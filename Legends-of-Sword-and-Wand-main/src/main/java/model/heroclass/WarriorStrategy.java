package model.heroclass;

import model.Ability;
import model.Hero;
import model.HeroClassStrategy;
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
    public List<Ability> getAbilities() {
        return Arrays.asList(new BerserkerAttack());
    }

    @Override
    public String getClassName() {
        return "Warrior";
    }
}
