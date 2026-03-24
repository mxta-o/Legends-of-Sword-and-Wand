package service;

import model.Hero;
import model.InnItem;
import model.Profile;

import java.util.List;

/**
 * Service interface for Inn visits (UC7).
 *
 * Responsibilities:
 *   - Auto-revive and fully restore all heroes on arrival (free)
 *   - Allow purchase of food/drink items from the shop
 *   - Offer unemployed heroes for recruitment (first 10 rooms only)
 */
public interface InnService {

    /**
     * Entry point for an inn visit.
     * Revives all heroes, fully restores HP and mana — no charge.
     *
     * @param profile the player profile whose active party is restored
     */
    void visitInn(Profile profile);

    /**
     * Purchase an item from the inn shop and apply its effect to the target hero.
     *
     * @param profile    the buyer's profile (gold is deducted here)
     * @param item       the item to buy
     * @param targetHero the hero who receives the item's effect
     * @return true if the purchase succeeded; false if the player cannot afford it
     */
    boolean purchaseItem(Profile profile, InnItem item, Hero targetHero);

    /**
     * Returns the list of recruitable heroes available at this inn.
     * Only populated when the current campaign room is ≤ 10 and the party has < 5 heroes.
     *
     * @param profile    the current player profile
     * @param roomNumber the 1-based room number (heroes only available in rooms 1-10)
     * @return a (possibly empty) list of recruitable hero candidates
     */
    List<Hero> getRecruitableHeroes(Profile profile, int roomNumber);

    /**
     * Attempt to recruit a hero candidate into the player's active party.
     * Level-1 heroes are free; higher levels cost 200g × level.
     *
     * @param profile    the recruiting player's profile
     * @param candidate  a hero returned by {@link #getRecruitableHeroes}
     * @return true if recruitment succeeded; false if party is full or player cannot afford it
     */
    boolean recruitHero(Profile profile, Hero candidate);
}
