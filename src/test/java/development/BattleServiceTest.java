package development;

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

    @Test
    public void testStrongerTeamWins() {
        Hero strong = new Hero("Strong", HeroClass.CHAOS);
        for (int i = 0; i < 10; i++) strong.levelUp(HeroClass.CHAOS);

        Hero weak = new Hero("Weak", HeroClass.ORDER);

        BattleResult result = battleService.startBattle(
                Collections.singletonList(strong),
                Collections.singletonList(weak));

        assertFalse(result.isDraw());
        assertTrue(result.getWinningTeam().contains(strong));
        assertTrue(result.getLosingTeam().contains(weak));
    }

    @Test
    public void testBattleEndsWhenTeamWipedOut() {
        Hero a = new Hero("Alpha", HeroClass.CHAOS);
        for (int i = 0; i < 8; i++) a.levelUp(HeroClass.CHAOS);

        Hero b = new Hero("Beta", HeroClass.ORDER);

        BattleResult result = battleService.startBattle(
                Collections.singletonList(a),
                Collections.singletonList(b));

        boolean teamADead = result.getLosingTeam().stream().noneMatch(Hero::isAlive);
        boolean teamBDead = result.getLosingTeam().stream().noneMatch(Hero::isAlive);
        assertTrue(teamADead || teamBDead);
    }

    @Test
    public void testDrawWhenTurnLimitReached() {
        Hero a = new Hero("TankA", HeroClass.ORDER);
        Hero b = new Hero("TankB", HeroClass.ORDER);
        for (int i = 0; i < 15; i++) {
            a.levelUp(HeroClass.ORDER);
            b.levelUp(HeroClass.ORDER);
        }

        BattleResult result = battleService.startBattle(
                Collections.singletonList(a),
                Collections.singletonList(b));

        assertTrue(result.isDraw());
        assertTrue(result.getWinningTeam().isEmpty());
        assertTrue(result.getLosingTeam().isEmpty());
    }

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

    @Test
    public void testBattleResultReferencesCorrectTeams() {
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
