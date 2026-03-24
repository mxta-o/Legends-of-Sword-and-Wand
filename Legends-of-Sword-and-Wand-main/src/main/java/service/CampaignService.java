package service;

import model.CampaignResult;
import model.Profile;

/**
 * Service interface for the PvE Campaign mode (M3).
 *
 * A campaign consists of up to 30 rooms.  Each room is either:
 *   - a BATTLE encounter (enemy party is generated and fought), or
 *   - an INN visit (UC7 is triggered automatically).
 *
 * Encounter probability:
 *   base 60% battle / 40% inn, shifted +3% toward battle per 10 cumulative
 *   hero levels, capped at 90% battle / 10% inn.
 *
 * After 30 rooms the campaign ends and a final score is calculated:
 *   score = (Σ hero levels × 100) + (gold × 10) + (Σ items bought × itemCost / 2 × 10)
 */
public interface CampaignService {

    /**
     * Starts a new campaign for the given profile.
     * Resets room counter and campaign score.
     */
    void startCampaign(Profile profile);

    /**
     * Advances the campaign by one room.
     * Determines room type, runs the encounter, distributes rewards or penalties,
     * and returns a CampaignResult describing what happened.
     *
     * @param profile the active player profile
     * @return the result of this room's encounter
     */
    CampaignResult enterNextRoom(Profile profile);

    /**
     * Calculates and returns the campaign's final score.
     * Should be called after 30 rooms are completed.
     *
     * @param profile the active player profile
     * @return the computed final score
     */
    int calculateFinalScore(Profile profile);

    /**
     * Returns whether the campaign has been completed (30 rooms done).
     *
     * @param profile the active player profile
     * @return true if campaign is over
     */
    boolean isCampaignComplete(Profile profile);
}
