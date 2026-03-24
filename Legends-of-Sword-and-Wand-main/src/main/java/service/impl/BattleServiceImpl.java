package service.impl;

import service.BattleService;
import model.Ability;
import model.Hero;
import model.BattleResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implementation of BattleService. Orchestrates the battle flow between teams.
 * Contains orchestration logic only; business rules remain in domain classes.
 * With the use of AI
 */
public class BattleServiceImpl implements BattleService {

    @Override
    public BattleResult startBattle(List<Hero> teamA, List<Hero> teamB) {
        List<Hero> teamACopy = new ArrayList<>(teamA);
        List<Hero> teamBCopy = new ArrayList<>(teamB);
        boolean isDraw = false;
        int turn = 0;

        while (isTeamAlive(teamACopy) && isTeamAlive(teamBCopy)) {
            // Tick status effects at the start of each round
            tickStatusEffects(teamACopy);
            tickStatusEffects(teamBCopy);

            // Sort each team: highest level first; ties broken by highest attack
            sortByInitiative(teamACopy);
            sortByInitiative(teamBCopy);

            // Teams alternate — teamA hero acts, then teamB hero, etc.
            takeTurn(teamACopy, teamBCopy);
            takeTurn(teamBCopy, teamACopy);

            turn++;
            if (turn > 1000) {
                isDraw = true;
                break;
            }
        }

        List<Hero> winningTeam = isTeamAlive(teamACopy)
                ? teamACopy
                : (isTeamAlive(teamBCopy) ? teamBCopy : new ArrayList<>());
        List<Hero> losingTeam = isTeamAlive(teamACopy) ? teamBCopy : teamACopy;

        if (isDraw) {
            winningTeam = new ArrayList<>();
            losingTeam  = new ArrayList<>();
        }
        return new BattleResult(winningTeam, losingTeam, isDraw);
    }

    // -------------------------------------------------------------------------
    // Turn execution
    // -------------------------------------------------------------------------

    /**
     * Executes one full turn for the acting team.
     * Heroes who choose WAIT are deferred and act at the end in FIFO order.
     * Stunned heroes are skipped and have their stun cleared for next turn.
     */
    private void takeTurn(List<Hero> actingTeam, List<Hero> opposingTeam) {
        Queue<Hero> waitQueue = new LinkedList<>();

        for (Hero hero : actingTeam) {
            if (!hero.isAlive()) continue;
            if (hero.isStunned()) continue; // skip stunned heroes entirely this turn

            Action action = decideAction(hero, opposingTeam);

            switch (action) {
                case ATTACK:
                    performAttack(hero, opposingTeam);
                    break;
                case DEFEND:
                    hero.defend();
                    break;
                case CAST:
                    performCast(hero, actingTeam, opposingTeam);
                    break;
                case WAIT:
                    hero.waitTurn();
                    waitQueue.add(hero);
                    break;
            }

            // Stop if the opposing team has been wiped out mid-turn
            if (!isTeamAlive(opposingTeam)) break;
        }

        // Process deferred (waiting) heroes in FIFO order
        while (!waitQueue.isEmpty()) {
            Hero hero = waitQueue.poll();
            if (!hero.isAlive() || !isTeamAlive(opposingTeam)) break;
            performAttack(hero, opposingTeam); // Waiting heroes default to attacking
        }
    }

    // -------------------------------------------------------------------------
    // AI decision logic
    // -------------------------------------------------------------------------

    /**
     * Simple AI: cast if mana allows and an ability is available, otherwise attack.
     * Defend if HP is critically low (below 25%) and no ability can be cast.
     */
    private Action decideAction(Hero hero, List<Hero> enemies) {
        List<Ability> abilities = hero.getClassAbilities();
        for (Ability ability : abilities) {
            if (hero.canCast(ability)) {
                return Action.CAST;
            }
        }
        double hpPercent = (double) hero.getCurrentHealth() / hero.getCurrentMaxHealth();
        if (hpPercent < 0.25) {
            return Action.DEFEND;
        }
        return Action.ATTACK;
    }

    // -------------------------------------------------------------------------
    // Action implementations
    // -------------------------------------------------------------------------

    /** Attacks the first alive enemy target. */
    private void performAttack(Hero hero, List<Hero> enemies) {
        Hero target = getFirstAliveTarget(enemies);
        if (target != null) {
            hero.attack(target);
        }
    }

    /** Casts the first ability the hero has enough mana for. */
    private void performCast(Hero hero, List<Hero> allies, List<Hero> enemies) {
        for (Ability ability : hero.getClassAbilities()) {
            if (hero.canCast(ability)) {
                // Healing/support abilities target allies; damage abilities target enemies
                List<Hero> targets = isSupportAbility(ability) ? allies : enemies;
                hero.castAbility(ability, targets);
                return;
            }
        }
        // Fallback: if somehow nothing can be cast, attack instead
        performAttack(hero, enemies);
    }

    /**
     * Determines whether an ability is a support (ally-targeting) ability
     * by checking its name against known support ability names.
     */
    private boolean isSupportAbility(Ability ability) {
        String name = ability.getName();
        return name.equals("Protect") || name.equals("Heal") || name.equals("Replenish");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isTeamAlive(List<Hero> team) {
        for (Hero hero : team) {
            if (hero.isAlive()) return true;
        }
        return false;
    }

    /** Returns the first alive hero in the list, or null if none. */
    private Hero getFirstAliveTarget(List<Hero> team) {
        for (Hero hero : team) {
            if (hero.isAlive()) return hero;
        }
        return null;
    }

    /** Sorts descending by level, then by attack as a tiebreaker. */
    private void sortByInitiative(List<Hero> team) {
        team.sort(Comparator
                .comparingInt(Hero::getLevel).reversed()
                .thenComparingInt(Hero::getCurrentAttack).reversed());
    }

    /** Ticks status effects for all alive heroes on a team. */
    private void tickStatusEffects(List<Hero> team) {
        for (Hero hero : team) {
            if (hero.isAlive()) hero.processStatusEffects();
        }
    }

    // -------------------------------------------------------------------------
    // Action enum
    // -------------------------------------------------------------------------

    private enum Action { ATTACK, DEFEND, CAST, WAIT }
}

