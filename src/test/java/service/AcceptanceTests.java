package service;

import model.*;
import model.ability.Protect;
import model.ability.Replenish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.InMemoryLeagueRepository;
import persistence.InMemoryProfileRepository;
import service.impl.BattleServiceImpl;
import service.impl.CampaignServiceImpl;
import service.impl.InnServiceImpl;
import service.impl.ProfileServiceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated JUnit test class for acceptance testing of major use cases.
 */
public class AcceptanceTests {

    private InMemoryProfileRepository profileRepo;
    private ProfileService profileService;
    private InnServiceImpl innService;
    private BattleService battleService;
    private CampaignService campaignService;
    private InMemoryLeagueRepository leagueRepo;

    @BeforeEach
    void setUp() {
        profileRepo    = new InMemoryProfileRepository();
        profileService = new ProfileServiceImpl(profileRepo);
        innService     = new InnServiceImpl();
        battleService  = new BattleServiceImpl();
        // seed campaign RNG for deterministic behavior where needed
        campaignService = new CampaignServiceImpl(battleService, innService, new Random(42L));
        leagueRepo     = new InMemoryLeagueRepository();
        LeagueService.resetForTesting();
    }

    @Test @DisplayName("TC-A01: Profile creation & persistence")
    void tcA01_createProfile_persists() {
        Profile p = profileService.createProfile("Alice-Acc");
        assertNotNull(p);
        assertEquals("Alice-Acc", p.getPlayerName());
        assertTrue(profileRepo.exists("Alice-Acc"));
    }

    @Test @DisplayName("TC-A02: Hero creation, add & party size enforcement")
    void tcA02_heroCreation_and_partyLimit() {
        Profile p = profileService.createProfile("Bob-Acc");
        for (int i = 0; i < 5; i++) profileService.createAndAddHero(p, "H" + i, HeroClass.ORDER);
        assertEquals(5, p.getActiveParty().size());
        // adding a sixth should return null
        Hero extra = profileService.createAndAddHero(p, "Extra", HeroClass.MAGE);
        assertNull(extra);
    }

    @Test @DisplayName("TC-A03: Recruit level-1 hero is free")
    void tcA03_recruitLevel1_free() {
        Profile p = profileService.createProfile("Cara-Acc");
        Hero candidate = new Hero("Wander", HeroClass.MAGE);
        boolean ok = innService.recruitHero(p, candidate);
        assertTrue(ok);
        assertEquals(1, p.getActiveParty().size());
    }

    @Test @DisplayName("TC-A04: Shop purchase effect & gold deduction")
    void tcA04_purchaseItem_and_gold() {
        Profile p = profileService.createProfile("Dora-Acc");
        p.addGold(300);
        Hero h = new Hero("Pal", HeroClass.WARRIOR);
        p.addHeroToParty(h);
        int before = h.getCurrentHealth();
        boolean bought = innService.purchaseItem(p, InnItem.BREAD, h);
        assertTrue(bought);
        assertEquals(100, p.getGold());
        int expected = Math.min(before + InnItem.BREAD.getHpRestore(), h.getCurrentMaxHealth());
        assertEquals(expected, h.getCurrentHealth());
    }

    @Test @DisplayName("TC-A05: Inn visit revives and restores heroes")
    void tcA05_visitInn_revives() {
        Profile p = profileService.createProfile("Eli-Acc");
        Hero h = new Hero("Tank", HeroClass.WARRIOR);
        h.takeDamage(100);
        p.addHeroToParty(h);
        assertFalse(h.isAlive());
        innService.visitInn(p);
        assertTrue(h.isAlive());
        assertEquals(h.getCurrentMaxHealth(), h.getCurrentHealth());
    }

    @Test @DisplayName("TC-A06: Start campaign resets room and activates campaign")
    void tcA06_startCampaign_resetsRoom() {
        Profile p = profileService.createProfile("Fay-Acc");
        p.addHeroToParty(new Hero("R", HeroClass.CHAOS));
        campaignService.startCampaign(p);
        assertEquals(0, p.getCampaignRoom());
        assertTrue(p.isCampaignActive());
    }

    @Test @DisplayName("TC-A07: Enter next room advances campaign counter")
    void tcA07_enterNextRoom_advances() {
        Profile p = profileService.createProfile("Gus-Acc");
        p.addHeroToParty(new Hero("H", HeroClass.WARRIOR));
        campaignService.startCampaign(p);
        campaignService.enterNextRoom(p);
        assertEquals(1, p.getCampaignRoom());
    }

    @Test @DisplayName("TC-A08: Stronger team wins PvE battle")
    void tcA08_strongerTeamWins() {
        Hero strong = new Hero("StrongAcc", HeroClass.CHAOS);
        for (int i = 0; i < 8; i++) strong.levelUp(HeroClass.CHAOS);
        Hero weak = new Hero("WeakAcc", HeroClass.ORDER);
        BattleResult res = battleService.startBattle(Collections.singletonList(strong), Collections.singletonList(weak));
        assertFalse(res.isDraw());
        assertTrue(res.getWinningTeam().contains(strong));
    }

    @Test @DisplayName("TC-A09: Battle draw declared at turn limit")
    void tcA09_drawWhenTurnLimitReached() {
        Hero a = new Hero("TankA", HeroClass.ORDER);
        Hero b = new Hero("TankB", HeroClass.ORDER);
        for (int i = 0; i < 15; i++) { a.levelUp(HeroClass.ORDER); b.levelUp(HeroClass.ORDER); }
        BattleResult res = battleService.startBattle(Collections.singletonList(a), Collections.singletonList(b));
        assertTrue(res.isDraw());
    }

    @Test @DisplayName("TC-A10: Protect ability shields allies and consumes mana")
    void tcA10_protect_shieldsAllies() {
        Hero caster = new Hero("CasterOrd", HeroClass.ORDER);
        Hero a1 = new Hero("A1", HeroClass.WARRIOR);
        Hero a2 = new Hero("A2", HeroClass.CHAOS);
        List<Hero> team = Arrays.asList(caster, a1, a2);
        int manaBefore = caster.getCurrentMana();
        caster.castAbility(new Protect(), team);
        assertEquals(manaBefore - 25, caster.getCurrentMana());
        int expected = (int)(a1.getCurrentMaxHealth() * 0.10);
        assertEquals(expected, a1.getShieldAmount());
        assertEquals(expected, a2.getShieldAmount());
    }

    @Test @DisplayName("TC-A11: Replenish restores mana to caster and allies")
    void tcA11_replenish_restoresMana() {
        Hero caster = new Hero("MageAcc", HeroClass.MAGE);
        Hero ally = new Hero("PalAcc", HeroClass.WARRIOR);
        caster.useMana(30);
        ally.useMana(30);
        int beforeCaster = caster.getCurrentMana();
        int beforeAlly = ally.getCurrentMana();
        new Replenish().execute(caster, Arrays.asList(caster, ally));
        assertTrue(caster.getCurrentMana() >= beforeCaster);
        assertTrue(ally.getCurrentMana() >= beforeAlly);
    }

    @Test @DisplayName("TC-A12: LeagueService records wins and losses (Singleton)")
    void tcA12_league_recordsWinsLosses() {
        LeagueService.resetForTesting();
        LeagueService league = LeagueService.getInstance(leagueRepo);
        league.recordResult("WinAcc", "LoseAcc");
        assertEquals(1, league.getEntry("WinAcc").getWins());
        assertEquals(1, league.getEntry("LoseAcc").getLosses());
    }
}
