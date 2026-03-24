package model.heroclass;

import model.Ability;
import model.Hero;
import model.HeroClassStrategy;
import model.ability.Heal;
import model.ability.Protect;

import java.util.Arrays;
import java.util.List;

/**
 * Order class strategy.
 * Per-level bonus: +5 mana, +2 defense.
 * Abilities: Protect (25 mana), Heal (35 mana).
 * With the use of AI
 */
public class OrderStrategy implements HeroClassStrategy {

    @Override
    public void applyLevelBonus(Hero hero) {
        hero.addMaxMana(5);
        hero.addBaseDefense(2);
    }

    @Override
    public List<Ability> getAbilities() {
        return Arrays.asList(new Protect(), new Heal());
    }

    @Override
    public String getClassName() {
        return "Order";
    }
}
