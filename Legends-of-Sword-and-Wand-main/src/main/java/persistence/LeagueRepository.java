package persistence;

import model.LeagueEntry;

import java.util.List;

/**
 * DAO interface for the PvP league table.
 */
public interface LeagueRepository {

    /** Returns the entry for the given player, creating it if absent. */
    LeagueEntry findOrCreate(String playerName);

    /** Persists an updated league entry. */
    void save(LeagueEntry entry);

    /** Returns all entries sorted by wins desc, then losses asc. */
    List<LeagueEntry> findAllSorted();
}
