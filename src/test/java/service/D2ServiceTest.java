package service;

import controller.GameController;
import model.*;
import org.junit.jupiter.api.*;
import persistence.InMemoryLeagueRepository;
import persistence.InMemoryProfileRepository;
import service.impl.*;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Deliverable 2 features:
 *   - Profile creation and persistence
 *   - InnService (revival, item purchase, recruitment)
 *   - CampaignService (room progression, scoring, encounter probability)
 *   - LeagueService Singleton (PvP win/loss recording)
 *   - Observer pattern (hero events)
 */
class D2ServiceTest {

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------

    private InMemoryProfileRepository profileRepo;
    private ProfileService profileService;
    private InnServiceImpl innService;
    private BattleService battleService;

    @BeforeEach
    void setUp() {
        profileRepo    = new InMemoryProfileRepository();
        profileService = new ProfileServiceImpl(profileRepo);
        innService     = new InnServiceImpl();
        battleService  = new BattleServiceImpl();
        LeagueService.resetForTesting();
    }

    // =========================================================================
    // TC-D2-01  Profile creation
    // =========================================================================

    @Test
    @DisplayName("TC-D2-01: createProfile creates and persists a new profile")
    void createProfile_persistsNewProfile() {
        Profile p = profileService.createProfile("Alice");

        assertNotNull(p);
        assertEquals("Alice", p.getPlayerName());
        assertEquals(0, p.getGold());
        assertTrue(profileRepo.exists("Alice"));
    }

    @Test
    @DisplayName("TC-D2-02: createProfile rejects duplicate names")
    void createProfile_rejectsDuplicateName() {
        profileService.createProfile("Bob");
        assertThrows(IllegalArgumentException.class,
                () -> profileService.createProfile("Bob"));
    }

    @Test
    @DisplayName("TC-D2-03: createProfile rejects blank name")
    void createProfile_rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> profileService.createProfile("  "));
    }

    @Test
    @DisplayName("TC-D2-04: createAndAddHero adds hero to party")
    void createAndAddHero_addsHeroToProfile() {
        Profile p = profileService.createProfile("Carol");
        Hero hero = profileService.createAndAddHero(p, "Lancelot", HeroClass.WARRIOR);

        assertNotNull(hero);
        assertEquals(1, p.getActiveParty().size());
        assertEquals("Lancelot", p.getActiveParty().get(0).getName());
    }

    @Test
    @DisplayName("TC-D2-05: active party limited to 5 heroes")
    void createAndAddHero_partyLimitedToFive() {
        Profile p = profileService.createProfile("Dave");
        for (int i = 0; i < 5; i++) {
            profileService.createAndAddHero(p, "Hero-" + i, HeroClass.ORDER);
        }
        Hero overflow = profileService.createAndAddHero(p, "Sixth", HeroClass.CHAOS);

        assertNull(overflow);
        assertEquals(5, p.getActiveParty().size());
    }

    // =========================================================================
    // TC-D2-06  Gold management
    // =========================================================================

    @Test
    @DisplayName("TC-D2-06: addGold / spendGold work correctly")
    void gold_addAndSpend() {
        Profile p = profileService.createProfile("Eve");
        p.addGold(500);
        assertEquals(500, p.getGold());

        boolean spent = p.spendGold(200);
        assertTrue(spent);
        assertEquals(300, p.getGold());
    }

    @Test
    @DisplayName("TC-D2-07: spendGold fails when insufficient funds")
    void gold_spendFails_insufficientFunds() {
        Profile p = profileService.createProfile("Frank");
        p.addGold(100);

        boolean result = p.spendGold(200);

        assertFalse(result);
        assertEquals(100, p.getGold()); // unchanged
    }

    @Test
    @DisplayName("TC-D2-08: applyGoldPenalty deducts 10%")
    void gold_penaltyDeductsTenPercent() {
        Profile p = profileService.createProfile("Grace");
        p.addGold(1000);
        p.applyGoldPenalty();

        assertEquals(900, p.getGold());
    }

    // =========================================================================
    // TC-D2-09  InnService — revival
    // =========================================================================

    @Test
    @DisplayName("TC-D2-09: visitInn revives dead heroes for free")
    void inn_visitRevivesDeadHeroes() {
        Profile p = profileService.createProfile("Hank");
        Hero hero = new Hero("Tank", HeroClass.WARRIOR);
        hero.takeDamage(100); // kill the hero
        p.addHeroToParty(hero);

        innService.visitInn(p);

        assertTrue(hero.isAlive());
        assertEquals(hero.getCurrentMaxHealth(), hero.getCurrentHealth());
        assertEquals(hero.getCurrentMaxMana(),   hero.getCurrentMana());
        assertEquals(0, p.getGold()); // no charge
    }

    // =========================================================================
    // TC-D2-10  InnService — item purchase
    // =========================================================================

    @Test
    @DisplayName("TC-D2-10: purchaseItem succeeds when player has enough gold")
    void inn_purchaseItemSucceeds() {
        Profile p = profileService.createProfile("Ivy");
        p.addGold(300);
        Hero hero = new Hero("Mage", HeroClass.MAGE);
        hero.takeDamage(30); // reduce HP so heal is meaningful
        p.addHeroToParty(hero);

        int hpBefore = hero.getCurrentHealth();
        boolean bought = innService.purchaseItem(p, InnItem.BREAD, hero);

        assertTrue(bought);
        assertEquals(100, p.getGold());              // 300 - 200
        assertEquals(hpBefore + 20, hero.getCurrentHealth()); // Bread +20 HP
    }

    @Test
    @DisplayName("TC-D2-11: purchaseItem fails when gold is insufficient")
    void inn_purchaseItemFails_noGold() {
        Profile p = profileService.createProfile("Jack");
        p.addGold(50); // not enough for Bread (200g)
        Hero hero = new Hero("Warrior", HeroClass.WARRIOR);

        boolean bought = innService.purchaseItem(p, InnItem.BREAD, hero);

        assertFalse(bought);
        assertEquals(50, p.getGold()); // unchanged
    }

    @Test
    @DisplayName("TC-D2-12: Elixir fully revives and restores a dead hero")
    void inn_elixirRevivesAndRestores() {
        Profile p = profileService.createProfile("Kim");
        p.addGold(2000);
        Hero hero = new Hero("Paladin", HeroClass.ORDER);
        hero.takeDamage(100);
        assertFalse(hero.isAlive());

        innService.purchaseItem(p, InnItem.ELIXIR, hero);

        assertTrue(hero.isAlive());
        assertEquals(hero.getCurrentMaxHealth(), hero.getCurrentHealth());
        assertEquals(hero.getCurrentMaxMana(),   hero.getCurrentMana());
        assertEquals(0, p.getGold());
    }

    // =========================================================================
    // TC-D2-13  InnService — recruitment
    // =========================================================================

    @Test
    @DisplayName("TC-D2-13: getRecruitableHeroes returns candidates in rooms 1-10")
    void inn_recruitablePresentInEarlyRooms() {
        Profile p = profileService.createProfile("Leo");
        // Party has room (< 5 heroes)
        List<Hero> candidates = innService.getRecruitableHeroes(p, 5);
        assertFalse(candidates.isEmpty());
        assertTrue(candidates.size() <= 3);
    }

    @Test
    @DisplayName("TC-D2-14: getRecruitableHeroes returns empty after room 10")
    void inn_noRecruitableAfterRoomTen() {
        Profile p = profileService.createProfile("Mia");
        List<Hero> candidates = innService.getRecruitableHeroes(p, 11);
        assertTrue(candidates.isEmpty());
    }

    @Test
    @DisplayName("TC-D2-15: recruitHero (level 1) is free")
    void inn_recruitLevelOneIsFree() {
        Profile p = profileService.createProfile("Ned");
        p.addGold(0);
        Hero candidate = new Hero("Wanderer", HeroClass.MAGE); // level 1

        boolean recruited = innService.recruitHero(p, candidate);

        assertTrue(recruited);
        assertEquals(1, p.getActiveParty().size());
        assertEquals(0, p.getGold()); // no charge
    }

    @Test
    @DisplayName("TC-D2-16: recruitHero (level 3) costs 600g")
    void inn_recruitLevel3Costs600Gold() {
        Profile p = profileService.createProfile("Ora");
        p.addGold(600);
        Hero candidate = new Hero("Veteran", HeroClass.WARRIOR);
        candidate.levelUp(HeroClass.WARRIOR);
        candidate.levelUp(HeroClass.WARRIOR); // now level 3

        boolean recruited = innService.recruitHero(p, candidate);

        assertTrue(recruited);
        assertEquals(0, p.getGold());
        assertEquals(1, p.getActiveParty().size());
    }

    @Test
    @DisplayName("TC-D2-17: recruitHero fails when party is full")
    void inn_recruitFailsWhenPartyFull() {
        Profile p = profileService.createProfile("Pete");
        for (int i = 0; i < 5; i++) p.addHeroToParty(new Hero("H" + i, HeroClass.ORDER));

        Hero candidate = new Hero("Extra", HeroClass.MAGE);
        boolean recruited = innService.recruitHero(p, candidate);

        assertFalse(recruited);
        assertEquals(5, p.getActiveParty().size());
    }

    // =========================================================================
    // TC-D2-18  CampaignService
    // =========================================================================

    @Test
    @DisplayName("TC-D2-18: startCampaign resets room counter to 0")
    void campaign_startResetsRoom() {
        CampaignService campaign = new CampaignServiceImpl(battleService, innService);
        Profile p = profileService.createProfile("Quinn");
        p.addHeroToParty(new Hero("Ranger", HeroClass.CHAOS));

        campaign.startCampaign(p);

        assertEquals(0, p.getCampaignRoom());
        assertTrue(p.isCampaignActive());
    }

    @Test
    @DisplayName("TC-D2-19: enterNextRoom advances campaign room counter")
    void campaign_enterNextRoomAdvancesCounter() {
        CampaignService campaign = new CampaignServiceImpl(battleService, innService);
        Profile p = profileService.createProfile("Rose");
        p.addHeroToParty(new Hero("Knight", HeroClass.WARRIOR));
        campaign.startCampaign(p);

        campaign.enterNextRoom(p);

        assertEquals(1, p.getCampaignRoom());
    }

    @Test
    @DisplayName("TC-D2-20: campaign ends after 30 rooms")
    void campaign_endsAfter30Rooms() {
        // Use a seeded random that always picks INN to avoid battles killing the party
        Random seededRandom = new Random(42L) {
            @Override public double nextDouble() { return 0.99; } // always > any battle %
        };
        CampaignService campaign = new CampaignServiceImpl(battleService, innService, seededRandom);
        Profile p = profileService.createProfile("Sam");
        p.addHeroToParty(new Hero("Cleric", HeroClass.ORDER));
        campaign.startCampaign(p);

        for (int i = 0; i < 30; i++) campaign.enterNextRoom(p);

        assertTrue(campaign.isCampaignComplete(p));
        assertFalse(p.isCampaignActive());
    }

    @Test
    @DisplayName("TC-D2-21: calculateFinalScore includes hero levels and gold")
    void campaign_finalScoreIncludesLevelsAndGold() {
        CampaignService campaign = new CampaignServiceImpl(battleService, innService);
        Profile p = profileService.createProfile("Tara");
        Hero hero = new Hero("Archer", HeroClass.CHAOS);
        p.addHeroToParty(hero); // level 1
        p.addGold(100);
        campaign.startCampaign(p);

        int score = campaign.calculateFinalScore(p);

        // level 1 → 100pts; gold 100 → 1000pts  → total ≥ 1100
        assertTrue(score >= 1100,
                "Expected score >= 1100 but was " + score);
    }

    @Test
    @DisplayName("TC-D2-22: isCampaignComplete returns false before 30 rooms")
    void campaign_notCompleteBeforeThirtyRooms() {
        CampaignService campaign = new CampaignServiceImpl(battleService, innService);
        Profile p = profileService.createProfile("Uma");
        p.addHeroToParty(new Hero("Scout", HeroClass.WARRIOR));
        campaign.startCampaign(p);

        assertFalse(campaign.isCampaignComplete(p));
    }

    // =========================================================================
    // TC-D2-23  LeagueService Singleton
    // =========================================================================

    @Test
    @DisplayName("TC-D2-23: LeagueService is a Singleton")
    void leagueService_isSingleton() {
        InMemoryLeagueRepository repo = new InMemoryLeagueRepository();
        LeagueService a = LeagueService.getInstance(repo);
        LeagueService b = LeagueService.getInstance(repo);

        assertSame(a, b);
    }

    @Test
    @DisplayName("TC-D2-24: recordResult updates win and loss counters")
    void leagueService_recordResultUpdatesCounters() {
        InMemoryLeagueRepository repo = new InMemoryLeagueRepository();
        LeagueService league = LeagueService.getInstance(repo);

        league.recordResult("Alice", "Bob");

        assertEquals(1, league.getEntry("Alice").getWins());
        assertEquals(0, league.getEntry("Alice").getLosses());
        assertEquals(0, league.getEntry("Bob").getWins());
        assertEquals(1, league.getEntry("Bob").getLosses());
    }

    @Test
    @DisplayName("TC-D2-25: getLeagueTable is sorted by wins descending")
    void leagueService_tableIsSortedByWins() {
        InMemoryLeagueRepository repo = new InMemoryLeagueRepository();
        LeagueService league = LeagueService.getInstance(repo);

        league.recordResult("Carol", "Dave");
        league.recordResult("Carol", "Eve");
        league.recordResult("Dave",  "Eve");

        List<model.LeagueEntry> table = league.getLeagueTable();
        assertEquals("Carol", table.get(0).getPlayerName()); // 2 wins
        assertEquals("Dave",  table.get(1).getPlayerName()); // 1 win
        assertEquals("Eve",   table.get(2).getPlayerName()); // 0 wins
    }

    // =========================================================================
    // TC-D2-26  Observer pattern
    // =========================================================================

    @Test
    @DisplayName("TC-D2-26: observer is notified when hero dies")
    void observer_notifiedOnDeath() {
        Hero hero = new Hero("Brave", HeroClass.WARRIOR);
        boolean[] died = { false };
        hero.addObserver(new HeroObserver() {
            @Override public void onHeroDied(Hero h)               { died[0] = true; }
            @Override public void onHeroRevived(Hero h)            {}
            @Override public void onLevelUp(Hero h, int lvl)       {}
            @Override public void onHeroHealed(Hero h, int amount) {}
        });

        hero.takeDamage(100);

        assertTrue(died[0]);
    }

    @Test
    @DisplayName("TC-D2-27: observer is notified on level-up")
    void observer_notifiedOnLevelUp() {
        Hero hero = new Hero("Scholar", HeroClass.MAGE);
        int[] capturedLevel = { 0 };
        hero.addObserver(new HeroObserver() {
            @Override public void onHeroDied(Hero h)               {}
            @Override public void onHeroRevived(Hero h)            {}
            @Override public void onLevelUp(Hero h, int lvl)       { capturedLevel[0] = lvl; }
            @Override public void onHeroHealed(Hero h, int amount) {}
        });

        hero.levelUp(HeroClass.MAGE);

        assertEquals(2, capturedLevel[0]);
    }

    @Test
    @DisplayName("TC-D2-28: observer is notified on revive")
    void observer_notifiedOnRevive() {
        Hero hero = new Hero("Phoenix", HeroClass.ORDER);
        hero.takeDamage(100);
        boolean[] revived = { false };
        hero.addObserver(new HeroObserver() {
            @Override public void onHeroDied(Hero h)               {}
            @Override public void onHeroRevived(Hero h)            { revived[0] = true; }
            @Override public void onLevelUp(Hero h, int lvl)       {}
            @Override public void onHeroHealed(Hero h, int amount) {}
        });

        hero.revive();

        assertTrue(revived[0]);
    }

    @Test
    @DisplayName("TC-D2-29: observer is notified on heal")
    void observer_notifiedOnHeal() {
        Hero hero = new Hero("Healer", HeroClass.ORDER);
        hero.takeDamage(30);
        int[] healedAmt = { 0 };
        hero.addObserver(new HeroObserver() {
            @Override public void onHeroDied(Hero h)               {}
            @Override public void onHeroRevived(Hero h)            {}
            @Override public void onLevelUp(Hero h, int lvl)       {}
            @Override public void onHeroHealed(Hero h, int amount) { healedAmt[0] = amount; }
        });

        hero.heal(20);

        assertEquals(20, healedAmt[0]);
    }

    @Test
    @DisplayName("TC-D2-30: Hall of Fame returns profiles sorted by high score")
    void hallOfFame_sortedByHighScore() {
        GameController game = new GameController();

        Profile top = game.createProfile("HoF-Top");
        top.endCampaign(5000);
        game.save();

        Profile low = game.createProfile("HoF-Low");
        low.endCampaign(1000);
        game.save();

        List<Profile> hallOfFame = game.getHallOfFame();

        assertTrue(hallOfFame.size() >= 2);
        assertEquals("HoF-Top", hallOfFame.get(0).getPlayerName());
    }
}
