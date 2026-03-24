package service;

import model.BattleResult;
import model.Hero;
import model.HeroClass;
import org.junit.jupiter.api.Test;
import service.impl.BattleServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BattleServiceTest {

    private final BattleServiceImpl battleService = new BattleServiceImpl();

    // =========================================================================
    // 1. Stronger team wins
    // =========================================================================

    @Test
    public void testStrongerTeamWins() {
        // Team A: one heavily levelled Chaos hero
        Hero strong = new Hero("Strong", HeroClass.CHAOS);
        for (int i = 0; i < 10; i++) strong.levelUp(HeroClass.CHAOS);

        // Team B: one fresh level-1 hero
        Hero weak = new Hero("Weak", HeroClass.ORDER);

        BattleResult result = battleService.startBattle(
                Collections.singletonList(strong),
                Collections.singletonList(weak));

        assertFalse(result.isDraw());
        assertTrue(result.getWinningTeam().contains(strong));
        assertTrue(result.getLosingTeam().contains(weak));
    }

    // =========================================================================
    // 2. Battle ends when a full team is wiped out
    // =========================================================================

    @Test
    public void testBattleEndsWhenTeamWipedOut() {
        Hero a = new Hero("Alpha", HeroClass.CHAOS);
        for (int i = 0; i < 8; i++) a.levelUp(HeroClass.CHAOS);

        Hero b = new Hero("Beta", HeroClass.ORDER);

        BattleResult result = battleService.startBattle(
                Collections.singletonList(a),
                Collections.singletonList(b));

        // At least one side must have no alive heroes
        boolean teamADead = result.getLosingTeam().stream().noneMatch(Hero::isAlive);
        boolean teamBDead = result.getLosingTeam().stream().noneMatch(Hero::isAlive);
        assertTrue(teamADead || teamBDead);
    }

    // =========================================================================
    // 3. Draw is declared after turn limit
    // =========================================================================

    @Test
    public void testDrawWhenTurnLimitReached() {
        // Two identically matched heroes that keep defending will exhaust the turn limit
        // We use a subclass trick: override to make them defend-loop until turn 1000.
        // Simpler: just give them both 0 ATK relative to each other's DEF by crafting
        // two ORDER heroes that are heavily defensive — they'll regenerate faster than
        // they take damage. Use enough levels so neither can kill the other in 1000 turns.
        Hero a = new Hero("TankA", HeroClass.ORDER);
        Hero b = new Hero("TankB", HeroClass.ORDER);
        for (int i = 0; i < 15; i++) {
            a.levelUp(HeroClass.ORDER); // +2 def/level => massive defense
            b.levelUp(HeroClass.ORDER);
        }

        BattleResult result = battleService.startBattle(
                Collections.singletonList(a),
                Collections.singletonList(b));

        assertTrue(result.isDraw());
        assertTrue(result.getWinningTeam().isEmpty());
        assertTrue(result.getLosingTeam().isEmpty());
    }

    // =========================================================================
    // 4. Multi-hero team — all members participate
    // =========================================================================

    @Test
    public void testMultiHeroTeamBattle() {
        Hero a1 = new Hero("A1", HeroClass.WARRIOR);
        Hero a2 = new Hero("A2", HeroClass.CHAOS);
        for (int i = 0; i < 5; i++) {
            a1.levelUp(HeroClass.WARRIOR);
            a2.levelUp(HeroClass.CHAOS);
        }

        Hero b1 = new Hero("B1", HeroClass.ORDER);
        Hero b2 = new Hero("B2", HeroClass.MAGE);

        BattleResult result = battleService.startBattle(
                Arrays.asList(a1, a2),
                Arrays.asList(b1, b2));

        assertNotNull(result);
        assertFalse(result.isDraw());
    }

    // =========================================================================
    // 5. Battle result contains references to the correct teams
    // =========================================================================

    @Test
    public void testBattleResultReferencesCorrectTeams() {
        // Verify the winning and losing team lists in the result correspond
        // to the teams passed in, not empty/null lists.
        Hero strong = new Hero("Strong2", HeroClass.CHAOS);
        for (int i = 0; i < 8; i++) strong.levelUp(HeroClass.CHAOS);

        Hero weak = new Hero("Weak2", HeroClass.ORDER);

        List<Hero> teamA = Collections.singletonList(strong);
        List<Hero> teamB = Collections.singletonList(weak);
        BattleResult result = battleService.startBattle(teamA, teamB);

        assertFalse(result.isDraw());
        assertFalse(result.getWinningTeam().isEmpty());
        assertFalse(result.getLosingTeam().isEmpty());
    }
}
