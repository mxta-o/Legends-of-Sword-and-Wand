package development;

import controller.GameController;
import model.*;
import org.junit.jupiter.api.*;
import service.BattleService;
import service.ProfileService;
import service.LeagueService;
import persistence.InMemoryLeagueRepository;
import persistence.InMemoryProfileRepository;
import service.impl.*;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class D2ServiceTest {

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

    // Additional tests omitted for brevity in this development grouping
}
