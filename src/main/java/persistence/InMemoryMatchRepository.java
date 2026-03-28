package persistence;

import model.PvpMatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory match repository fallback for when SQLite is unavailable.
 */
public class InMemoryMatchRepository implements MatchRepository {

    private final Map<String, List<String>> invites = new ConcurrentHashMap<>();
    private final Map<Long, PvpMatch> matches = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override
    public boolean insertInvite(String fromPlayer, String toPlayer) {
        invites.computeIfAbsent(toPlayer, k -> new CopyOnWriteArrayList<>()).add(fromPlayer);
        return true;
    }

    @Override
    public List<String> pollInvitesFor(String toPlayer) {
        List<String> l = invites.remove(toPlayer);
        return l == null ? List.of() : List.copyOf(l);
    }

    @Override
    public PvpMatch createMatch(PvpMatch match) {
        long id = idSeq.getAndIncrement();
        match.setId(id);
        match.setCreatedAt(Instant.now());
        match.setUpdatedAt(Instant.now());
        matches.put(id, match);
        return match;
    }

    @Override
    public void updateMatch(PvpMatch match) {
        match.setUpdatedAt(Instant.now());
        matches.put(match.getId(), match);
    }

    @Override
    public PvpMatch findById(long id) {
        return matches.get(id);
    }

    @Override
    public List<PvpMatch> findMatchesForPlayer(String playerName) {
        List<PvpMatch> out = new ArrayList<>();
        for (PvpMatch m : matches.values()) {
            if (playerName.equals(m.getPlayerA()) || playerName.equals(m.getPlayerB())) out.add(m);
        }
        return out;
    }
}
