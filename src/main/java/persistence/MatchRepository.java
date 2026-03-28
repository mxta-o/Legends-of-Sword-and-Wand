package persistence;

import model.PvpMatch;

import java.util.List;

public interface MatchRepository {
    boolean insertInvite(String fromPlayer, String toPlayer);
    List<String> pollInvitesFor(String toPlayer);

    PvpMatch createMatch(PvpMatch match);
    void updateMatch(PvpMatch match);
    PvpMatch findById(long id);
    List<PvpMatch> findMatchesForPlayer(String playerName);
}
