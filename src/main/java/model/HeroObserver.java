package model;

/**
 * Observer interface for significant hero events (DP5 — Observer pattern).
 *
 * Interested components (UI, campaign controller, persistence layer) implement
 * this interface and register themselves via {@link Hero#addObserver(HeroObserver)}.
 * The hero calls the appropriate method when an event occurs, keeping side effects
 * decoupled from the core domain logic.
 *
 * Events:
 *   - onHeroDied      — hero HP reached 0
 *   - onHeroRevived   — hero was revived from death
 *   - onLevelUp       — hero gained a level
 *   - onHeroHealed    — hero's HP was restored (non-trivial heal >= 1)
 */
public interface HeroObserver {

    /**
     * Called when a hero's HP drops to zero and isAlive becomes false.
     *
     * @param hero the hero that just died
     */
    void onHeroDied(Hero hero);

    /**
     * Called when a dead hero is revived.
     *
     * @param hero the hero that was revived
     */
    void onHeroRevived(Hero hero);

    /**
     * Called immediately after a hero levels up.
     *
     * @param hero     the hero that levelled up
     * @param newLevel the new level
     */
    void onLevelUp(Hero hero, int newLevel);

    /**
     * Called when a hero is healed.
     *
     * @param hero        the healed hero
     * @param amountHealed the actual HP restored (after clamping to max)
     */
    void onHeroHealed(Hero hero, int amountHealed);
}
