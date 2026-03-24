package persistence;

import model.LeagueEntry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory implementation of LeagueRepository.
 * Suitable for unit tests and as a stand-in before SQLite is wired up.
 */
public class InMemoryLeagueRepository implements LeagueRepository {

    private final Map<String, LeagueEntry> store = new LinkedHashMap<>();

    @Override
    public LeagueEntry findOrCreate(String playerName) {
        return store.computeIfAbsent(playerName, LeagueEntry::new);
    }

    @Override
    public void save(LeagueEntry entry) {
        store.put(entry.getPlayerName(), entry);
    }

    @Override
    public List<LeagueEntry> findAllSorted() {
        return store.values().stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
