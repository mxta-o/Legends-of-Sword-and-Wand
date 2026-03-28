package service;

import model.LeagueEntry;
import persistence.LeagueRepository;

import java.util.List;

/**
 * Singleton service that manages the PvP league table (DP4 — Singleton).
 *
 * There must be exactly one instance throughout the application lifetime so
 * that all PvP battle outcomes are recorded in the same table.
 *
 * Thread-safety: initialised via double-checked locking (safe for Java 5+
 * with the volatile keyword).
 *
 * Usage:
 *   LeagueService league = LeagueService.getInstance(repository);
 *   league.recordResult("Alice", "Bob");  // Alice won
 *   List<LeagueEntry> table = league.getLeagueTable();
 */
public class LeagueService {

    private static volatile LeagueService instance;

    private final LeagueRepository repository;

    // Private constructor — only getInstance() may create this object
    private LeagueService(LeagueRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the single LeagueService instance, creating it on the first call.
     *
     * @param repository the persistence back-end (ignored after the first call)
     */
    public static LeagueService getInstance(LeagueRepository repository) {
        if (instance == null) {
            synchronized (LeagueService.class) {
                if (instance == null) {
                    instance = new LeagueService(repository);
                }
            }
        }
        return instance;
    }

    /**
     * Records the result of a PvP match.
     *
     * @param winnerName the winning player's name
     * @param loserName  the losing player's name
     */
    public void recordResult(String winnerName, String loserName) {
        LeagueEntry winner = repository.findOrCreate(winnerName);
        LeagueEntry loser  = repository.findOrCreate(loserName);
        winner.recordWin();
        loser.recordLoss();
        repository.save(winner);
        repository.save(loser);
    }

    /**
     * Returns the full league table, sorted by wins desc, then losses asc.
     */
    public List<LeagueEntry> getLeagueTable() {
        return repository.findAllSorted();
    }

    /**
     * Returns the league entry for a single player, or null if they have no record.
     */
    public LeagueEntry getEntry(String playerName) {
        return repository.findOrCreate(playerName);
    }

    /**
     * Seed or update a single league entry with explicit win/loss counts.
     * Useful when rebuilding an in-memory league from persisted profile data.
     */
    public void seedEntry(String playerName, int wins, int losses) {
        LeagueEntry e = repository.findOrCreate(playerName);
        for (int i = 0; i < wins; i++) e.recordWin();
        for (int i = 0; i < losses; i++) e.recordLoss();
        repository.save(e);
    }

    /**
     * Resets the singleton (for use in unit tests ONLY).
     * Not to be called in production code.
     */
    public static void resetForTesting() {
        instance = null;
    }
}
