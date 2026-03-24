package persistence;

import model.Profile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ProfileRepository.
 *
 * Suitable for unit tests and as a drop-in substitute before a real
 * database backend (SQLite) is wired up.  All data is lost when the JVM exits.
 *
 * Thread-safety: not guaranteed — not needed for single-threaded console use.
 */
public class InMemoryProfileRepository implements ProfileRepository {

    private final Map<String, Profile> store = new LinkedHashMap<>();

    @Override
    public void insert(Profile profile) {
        if (store.containsKey(profile.getPlayerName())) {
            throw new IllegalArgumentException(
                    "Profile already exists: " + profile.getPlayerName());
        }
        store.put(profile.getPlayerName(), profile);
    }

    @Override
    public void update(Profile profile) {
        // In-memory: the profile object is already the same reference — no-op needed.
        // For consistency with the interface contract, re-put it.
        store.put(profile.getPlayerName(), profile);
    }

    @Override
    public Profile findByName(String playerName) {
        return store.get(playerName);
    }

    @Override
    public List<Profile> findAllByHighScoreDesc() {
        return store.values().stream()
                .sorted(Comparator.comparingInt(Profile::getHighScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByName(String playerName) {
        store.remove(playerName);
    }

    @Override
    public boolean exists(String playerName) {
        return store.containsKey(playerName);
    }

    /** Returns total number of stored profiles (useful in tests). */
    public int count() {
        return store.size();
    }
}
