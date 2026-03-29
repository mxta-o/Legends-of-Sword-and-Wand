package service.impl;

import model.Hero;
import model.Ability;
import model.BattleResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Centralized battle engine used by UI and services.
 */
public class BattleEngine {

    public static List<Hero> buildTurnOrder(List<Hero> party, List<Hero> enemies) {
        List<Hero> order = new ArrayList<>();
        if (party != null) {
            for (Hero h : party) if (h != null && h.isAlive()) order.add(h);
        }
        if (enemies != null) {
            for (Hero h : enemies) if (h != null && h.isAlive()) order.add(h);
        }
        order.sort(Comparator
                .comparingInt(Hero::getLevel).reversed()
                .thenComparingInt(Hero::getCurrentAttack).reversed());
        return order;
    }

    public static void tickStatuses(List<Hero> team) {
        if (team == null) return;
        for (Hero hero : team) {
            if (hero != null && hero.isAlive()) hero.processStatusEffects();
        }
    }

    public static boolean isTeamAlive(List<Hero> team) {
        if (team == null) return false;
        for (Hero h : team) if (h != null && h.isAlive()) return true;
        return false;
    }

    public static Hero firstAlive(List<Hero> team) {
        if (team == null) return null;
        for (Hero hero : team) {
            if (hero != null && hero.isAlive()) return hero;
        }
        return null;
    }

    public static Hero firstAliveExcept(List<Hero> list, Hero exclude) {
        if (list == null) return null;
        for (Hero h : list) {
            if (h != null && h.isAlive() && h != exclude) return h;
        }
        return null;
    }

    public static List<Hero> aliveMembers(List<Hero> team) {
        List<Hero> alive = new ArrayList<>();
        if (team == null) return alive;
        for (Hero hero : team) {
            if (hero != null && hero.isAlive()) alive.add(hero);
        }
        return alive;
    }

    public static List<Hero> deepCopyParty(List<Hero> src) {
        List<Hero> out = new ArrayList<>();
        if (src == null) return out;
        for (Hero h : src) {
            if (h == null) continue;
            Hero copy = new Hero(h.getName(), h.getHeroClass());
            for (model.HeroClass cls : model.HeroClass.values()) {
                int lvl = h.getClassLevel(cls);
                if (cls == copy.getHeroClass()) {
                    for (int i = 1; i < lvl; i++) copy.levelUp(cls);
                } else {
                    for (int i = 0; i < lvl; i++) copy.levelUp(cls);
                }
            }
            int desiredHp = h.getCurrentHealth();
            int maxHp = copy.getCurrentMaxHealth();
            if (desiredHp < maxHp) copy.takeDamage(Math.max(0, maxHp - desiredHp));
            int desiredMana = h.getCurrentMana();
            int maxMana = copy.getCurrentMaxMana();
            if (desiredMana < maxMana) copy.useMana(Math.max(0, maxMana - desiredMana));
            copy.setShieldAmount(h.getShieldAmount());
            if (!h.isAlive() && copy.isAlive()) copy.takeDamage(copy.getCurrentHealth());
            out.add(copy);
        }
        return out;
    }

    public static BattleResult runBattle(List<Hero> teamA, List<Hero> teamB) {
        List<Hero> teamACopy = new ArrayList<>(teamA);
        List<Hero> teamBCopy = new ArrayList<>(teamB);
        boolean isDraw = false;
        int turn = 0;
        int consecutiveNoDamageRounds = 0;

        while (isTeamAlive(teamACopy) && isTeamAlive(teamBCopy)) {
            tickStatuses(teamACopy);
            tickStatuses(teamBCopy);
            sortByInitiative(teamACopy);
            sortByInitiative(teamBCopy);
            int totalHpBefore = totalTeamHp(teamACopy) + totalTeamHp(teamBCopy);
            takeTurn(teamACopy, teamBCopy);
            takeTurn(teamBCopy, teamACopy);
            int totalHpAfter = totalTeamHp(teamACopy) + totalTeamHp(teamBCopy);
            if (totalHpAfter == totalHpBefore) consecutiveNoDamageRounds++; else consecutiveNoDamageRounds = 0;
            if (consecutiveNoDamageRounds >= 50) { isDraw = true; break; }
            turn++; if (turn > 1000) { isDraw = true; break; }
        }

        List<Hero> winningTeam = isTeamAlive(teamACopy) ? teamACopy : (isTeamAlive(teamBCopy) ? teamBCopy : new ArrayList<>());
        List<Hero> losingTeam = isTeamAlive(teamACopy) ? teamBCopy : teamACopy;
        if (isDraw) { winningTeam = new ArrayList<>(); losingTeam = new ArrayList<>(); }
        return new BattleResult(winningTeam, losingTeam, isDraw);
    }

    private static void takeTurn(List<Hero> actingTeam, List<Hero> opposingTeam) {
        Queue<Hero> waitQueue = new LinkedList<>();
        for (Hero hero : actingTeam) {
            if (!hero.isAlive()) continue;
            if (hero.isStunned()) continue;
            Action action = decideAction(hero, opposingTeam);
            switch (action) {
                case ATTACK: performAttack(hero, opposingTeam); break;
                case DEFEND: hero.defend(); break;
                case CAST: performCast(hero, actingTeam, opposingTeam); break;
                case WAIT: hero.waitTurn(); waitQueue.add(hero); break;
            }
            if (!isTeamAlive(opposingTeam)) break;
        }
        while (!waitQueue.isEmpty()) {
            Hero hero = waitQueue.poll();
            if (!hero.isAlive() || !isTeamAlive(opposingTeam)) break;
            performAttack(hero, opposingTeam);
        }
    }

    private static Action decideAction(Hero hero, List<Hero> enemies) {
        for (Ability ability : hero.getClassAbilities()) if (hero.canCast(ability)) return Action.CAST;
        double hpPercent = (double) hero.getCurrentHealth() / Math.max(1, hero.getCurrentMaxHealth());
        if (hpPercent < 0.25) return Action.DEFEND;
        return Action.ATTACK;
    }

    private static void performAttack(Hero hero, List<Hero> enemies) {
        Hero target = getFirstAliveTarget(enemies);
        if (target != null) hero.attack(target);
    }

    private static void performCast(Hero hero, List<Hero> allies, List<Hero> enemies) {
        for (Ability ability : hero.getClassAbilities()) {
            if (hero.canCast(ability)) {
                List<Hero> targets = isSupportAbility(ability) ? allies : enemies;
                hero.castAbility(ability, targets);
                return;
            }
        }
        performAttack(hero, enemies);
    }

    private static boolean isSupportAbility(Ability ability) {
        String name = ability.getName();
        return name.equals("Protect") || name.equals("Heal") || name.equals("Replenish");
    }

    private static Hero getFirstAliveTarget(List<Hero> team) {
        if (team == null) return null;
        for (Hero hero : team) if (hero != null && hero.isAlive()) return hero;
        return null;
    }

    private static void sortByInitiative(List<Hero> team) {
        if (team == null) return;
        team.sort(Comparator
                .comparingInt(Hero::getLevel).reversed()
                .thenComparingInt(Hero::getCurrentAttack).reversed());
    }

    private static int totalTeamHp(List<Hero> team) {
        int sum = 0;
        if (team == null) return 0;
        for (Hero h : team) if (h != null) sum += h.getCurrentHealth();
        return sum;
    }

    private enum Action { ATTACK, DEFEND, CAST, WAIT }
}
