package model;

import java.util.List;

/**
 * Strategy interface for hero class behaviour (Strategy pattern).
 *
 * Each concrete class (Order, Chaos, Warrior, Mage) implements this interface
 * and encapsulates its own level-up bonus and available abilities.
 * Hero holds a HeroClassStrategy instance alongside the HeroClass enum key,
 * keeping the type-identity (used in classLevels map) separate from behaviour.
 */
public interface HeroClassStrategy {

    /**
     * Apply this class's per-level stat bonus to the given hero.
     * Called by Hero.levelUp() after base stat growth is applied.
     */
    void applyLevelBonus(Hero hero);

    /**
     * Returns the list of abilities available to this class.
     */
    List<Ability> getAbilities();

    /**
     * Returns the display name of this class.
     */
    String getClassName();
}
