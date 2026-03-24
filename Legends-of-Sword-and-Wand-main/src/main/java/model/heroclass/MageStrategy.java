package model.heroclass;

import model.Ability;
import model.Hero;
import model.HeroClassStrategy;
import model.ability.Replenish;

import java.util.Arrays;
import java.util.List;

/**
 * Mage class strategy.
 * Per-level bonus: +5 mana, +1 attack.
 * Abilities: Replenish (80 mana).
 * With the use of AI
 */
public class MageStrategy implements HeroClassStrategy {

    @Override
    public void applyLevelBonus(Hero hero) {
        hero.addMaxMana(5);
        hero.addBaseAttack(1);
    }

    @Override
    public List<Ability> getAbilities() {
        return Arrays.asList(new Replenish());
    }

    @Override
    public String getClassName() {
        return "Mage";
    }
}
