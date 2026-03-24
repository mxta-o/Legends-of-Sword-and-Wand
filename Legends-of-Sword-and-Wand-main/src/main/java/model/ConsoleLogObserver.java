package model;

/**
 * A simple HeroObserver that prints game events to the console.
 * Attach to any hero to get live battle commentary.
 *
 * Example:
 *   hero.addObserver(new ConsoleLogObserver());
 */
public class ConsoleLogObserver implements HeroObserver {

    @Override
    public void onHeroDied(Hero hero) {
        System.out.printf("  ☠  %s has fallen!%n", hero.getName());
    }

    @Override
    public void onHeroRevived(Hero hero) {
        System.out.printf("  ✨ %s has been revived! (HP: %d, Mana: %d)%n",
                hero.getName(), hero.getCurrentHealth(), hero.getCurrentMana());
    }

    @Override
    public void onLevelUp(Hero hero, int newLevel) {
        System.out.printf("  ⬆  %s reached level %d!%n", hero.getName(), newLevel);
    }

    @Override
    public void onHeroHealed(Hero hero, int amountHealed) {
        System.out.printf("  💚 %s recovered %d HP (now %d/%d)%n",
                hero.getName(), amountHealed,
                hero.getCurrentHealth(), hero.getCurrentMaxHealth());
    }
}
