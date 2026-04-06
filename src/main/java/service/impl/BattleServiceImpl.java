package service.impl;

import service.BattleService;
import model.Hero;
import model.BattleResult;

import java.util.List;

/**
 * Implementation of BattleService. Orchestrates the battle flow between teams.
 * Contains orchestration logic only; business rules remain in domain classes.
 * With the use of AI
 */
public class BattleServiceImpl implements BattleService {

    @Override
    public BattleResult startBattle(List<Hero> teamA, List<Hero> teamB) {
        return BattleEngine.runBattle(teamA, teamB);
    }
}

