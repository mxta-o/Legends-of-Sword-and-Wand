package service;

import model.Hero;
import model.HeroClass;
import model.Profile;

import java.util.List;

/**
 * Service interface for player profile creation and management.
 *
 * Responsibilities:
 *  - Create a new player profile
 *  - Add / remove heroes from the active party
 *  - Persist and load profiles (delegates to ProfileRepository)
 */
public interface ProfileService {

    /**
     * Creates and persists a brand-new profile with the given player name.
     *
     * @param playerName unique player name
     * @return the newly created Profile
     * @throws IllegalArgumentException if playerName is blank or already taken
     */
    Profile createProfile(String playerName);

    /**
     * Loads an existing profile by player name.
     *
     * @param playerName the player's name
     * @return the Profile, or null if not found
     */
    Profile loadProfile(String playerName);

    /**
     * Persists the current state of a profile.
     *
     * @param profile the profile to save
     */
    void saveProfile(Profile profile);

    /**
     * Returns all profiles sorted by high score descending.
     */
    List<Profile> getHallOfFame();

    /**
     * Creates a new hero and adds them to the profile's active party.
     *
     * @param profile   the player's profile
     * @param heroName  the hero's name
     * @param heroClass the chosen class
     * @return the newly created Hero, or null if the party is already full (5 heroes)
     */
    Hero createAndAddHero(Profile profile, String heroName, HeroClass heroClass);
}
