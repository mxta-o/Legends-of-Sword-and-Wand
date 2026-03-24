package model.heroclass;

import model.Ability;
import model.Hero;
import model.HeroClassStrategy;
import model.ability.ChainLightning;
import model.ability.Fireball;

import java.util.Arrays;
import java.util.List;

/**
 * Chaos class strategy.
 * Per-level bonus: +3 attack, +5 health.
 * Abilities: Fireball (30 mana), Chain Lightning (40 mana).
 * With the use of AI
 */
public class ChaosStrategy implements HeroClassStrategy {

    @Override
    public void applyLevelBonus(Hero hero) {
        hero.addBaseAttack(3);
        hero.addMaxHealth(5);
    }

    @Override
    public List<Ability> getAbilities() {
        return Arrays.asList(new Fireball(), new ChainLightning());
    }

    @Override
    public String getClassName() {
        return "Chaos";
    }
}
