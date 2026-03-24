package persistence;

import model.Profile;

import java.util.List;

/**
 * Data Access Object (DAO) interface for player profiles (M6 — Persistence).
 *
 * Abstracts all database operations so the service layer has no knowledge of
 * the underlying storage technology (SQLite, in-memory, file-based, etc.).
 *
 * Design pattern: Repository / DAO (DP6 — planned in SDD).
 */
public interface ProfileRepository {

    /**
     * Persists a new profile. Throws if the player name already exists.
     *
     * @param profile the profile to insert
     * @throws IllegalArgumentException if a profile with that name already exists
     */
    void insert(Profile profile);

    /**
     * Updates an existing profile record. No-op if the profile does not exist.
     *
     * @param profile the profile to update
     */
    void update(Profile profile);

    /**
     * Looks up a profile by player name.
     *
     * @param playerName the player's name (case-sensitive)
     * @return the matching Profile, or null if not found
     */
    Profile findByName(String playerName);

    /**
     * Returns all profiles, sorted by high score descending (for hall of fame).
     *
     * @return sorted list of all profiles
     */
    List<Profile> findAllByHighScoreDesc();

    /**
     * Deletes a profile by player name. No-op if not found.
     *
     * @param playerName the player's name
     */
    void deleteByName(String playerName);

    /**
     * Returns true if a profile with the given name already exists.
     *
     * @param playerName the player's name
     */
    boolean exists(String playerName);
}
