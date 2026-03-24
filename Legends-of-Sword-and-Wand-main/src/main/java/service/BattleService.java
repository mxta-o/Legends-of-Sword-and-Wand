package service;

import model.Hero;
import model.BattleResult;
import java.util.List;

/**
 * Service interface for orchestrating battles between teams of heroes.
 * Defines the contract for starting and executing battles.
 */
public interface BattleService {
    /**
     * Starts a battle between two teams of heroes.
     * @param teamA List of heroes in team A
     * @param teamB List of heroes in team B
     * @return BattleResult containing the outcome and final state
     */
    BattleResult startBattle(List<Hero> teamA, List<Hero> teamB);
}
