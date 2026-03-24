package service.impl;

import model.Hero;
import model.HeroClass;
import model.Profile;
import persistence.ProfileRepository;
import service.ProfileService;

/**
 * Concrete implementation of ProfileService.
 *
 * Delegates persistence to the injected ProfileRepository (DAO pattern),
 * keeping the service layer decoupled from storage technology.
 */
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository repository;

    public ProfileServiceImpl(ProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Profile createProfile(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be blank.");
        }
        if (repository.exists(playerName)) {
            throw new IllegalArgumentException(
                    "A profile with that name already exists: " + playerName);
        }
        Profile profile = new Profile(playerName);
        repository.insert(profile);
        return profile;
    }

    @Override
    public Profile loadProfile(String playerName) {
        return repository.findByName(playerName);
    }

    @Override
    public void saveProfile(Profile profile) {
        if (repository.exists(profile.getPlayerName())) {
            repository.update(profile);
        } else {
            repository.insert(profile);
        }
    }

    @Override
    public Hero createAndAddHero(Profile profile, String heroName, HeroClass heroClass) {
        if (profile.getActiveParty().size() >= 5) return null;
        Hero hero = new Hero(heroName, heroClass);
        profile.addHeroToParty(hero);
        repository.update(profile);
        return hero;
    }
}
