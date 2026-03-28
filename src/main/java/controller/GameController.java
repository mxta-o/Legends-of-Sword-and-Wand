package controller;

import model.*;
import persistence.InMemoryLeagueRepository;
import persistence.InMemoryProfileRepository;
import persistence.SQLiteProfileRepository;
import persistence.ProfileRepository;
import service.*;
import service.impl.*;

import java.util.List;

/**
 * Top-level application controller (Facade).
 *
 * Wires the full service graph together and exposes a clean API for the
 * presentation layer (console UI, future GUI, etc.) to call.
 *
 * This is the only class that should perform dependency injection.
 * All other classes receive their dependencies through their constructors.
 *
 * Singleton note: exactly one GameController should be created per JVM run.
 */
public class GameController {

    // -------------------------------------------------------------------------
    // Services (wired in constructor)
    // -------------------------------------------------------------------------

    private final ProfileService  profileService;
    private final BattleService   battleService;
    private final CampaignService campaignService;
    private final InnService      innService;
    private final LeagueService   leagueService;

    // Active session state
    private Profile currentProfile;

    // -------------------------------------------------------------------------
    // Constructor — default (in-memory) wiring for console / tests
    // -------------------------------------------------------------------------

    public GameController() {
        // Default to a local SQLite file-backed repository so profiles persist
        // across application restarts. If SQLite is unavailable at runtime the
        // controller will gracefully fall back to an in-memory repository.
        ProfileRepository profileRepo;
        InMemoryLeagueRepository  leagueRepo  = new InMemoryLeagueRepository();
            try {
            profileRepo = new SQLiteProfileRepository("game.db");
        } catch (RuntimeException ex) {
            System.err.println("SQLiteProfileRepository unavailable, falling back to in-memory: " + ex.getMessage());
            ex.printStackTrace(System.err);
            profileRepo = new InMemoryProfileRepository();
        }

        this.battleService   = new BattleServiceImpl();
        this.innService      = new InnServiceImpl();
        this.campaignService = new CampaignServiceImpl(battleService, innService);
        this.profileService  = new ProfileServiceImpl(profileRepo);
        this.leagueService   = LeagueService.getInstance(leagueRepo);
    }

    /**
     * Constructor for production use with a real database back-end.
     * Pass SQLiteProfileRepository / SQLiteLeagueRepository here.
     */
    public GameController(ProfileService profileService,
                          BattleService   battleService,
                          CampaignService campaignService,
                          InnService      innService,
                          LeagueService   leagueService) {
        this.profileService  = profileService;
        this.battleService   = battleService;
        this.campaignService = campaignService;
        this.innService      = innService;
        this.leagueService   = leagueService;
    }

    // -------------------------------------------------------------------------
    // Profile creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new player profile and sets it as the active session.
     *
     * @param playerName the desired player name
     * @return the new Profile
     * @throws IllegalArgumentException if the name is blank or already taken
     */
    public Profile createProfile(String playerName) {
        currentProfile = profileService.createProfile(playerName);
        return currentProfile;
    }

    /**
     * Create a new profile and set a password. Keeps the existing createProfile
     * method for backward compatibility.
     */
    public Profile createProfile(String playerName, String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        currentProfile = profileService.createProfile(playerName);
        if (currentProfile != null) {
            currentProfile.setPassword(password);
            profileService.saveProfile(currentProfile);
        }
        return currentProfile;
    }

    /**
     * Loads an existing profile and makes it the active session.
     *
     * @param playerName the existing player's name
     * @return the loaded Profile, or null if not found
     */
    public Profile loadProfile(String playerName) {
        currentProfile = profileService.loadProfile(playerName);
        return currentProfile;
    }

    /**
     * Load and authenticate a profile using a plaintext password.
     * Returns the profile on successful auth, or null on failure.
     */
    public Profile loadProfile(String playerName, String password) {
        Profile p = profileService.loadProfile(playerName);
        if (p == null) return null;
        if (!p.verifyPassword(password)) return null;
        currentProfile = p;
        return currentProfile;
    }

    public Profile getCurrentProfile() { return currentProfile; }

    // -------------------------------------------------------------------------
    // Hero management
    // -------------------------------------------------------------------------

    /**
     * Creates a hero and adds them to the active profile's party.
     *
     * @param heroName  the hero's name
     * @param heroClass the chosen class
     * @return the new Hero, or null if the party is full
     */
    public Hero createHero(String heroName, HeroClass heroClass) {
        requireProfile();
        Hero hero = profileService.createAndAddHero(currentProfile, heroName, heroClass);
        if (hero != null) {
            // Attach the console observer to all newly created heroes
            hero.addObserver(new ConsoleLogObserver());
        }
        return hero;
    }

    // -------------------------------------------------------------------------
    // Campaign
    // -------------------------------------------------------------------------

    /** Starts a new PvE campaign for the current profile. */
    public void startCampaign() {
        requireProfile();
        campaignService.startCampaign(currentProfile);
        System.out.println("Campaign started! 30 rooms await…");
    }

    /**
     * Advances the campaign by one room.
     *
     * @return the CampaignResult describing what happened
     */
    public CampaignResult enterNextRoom() {
        requireProfile();
        CampaignResult result = campaignService.enterNextRoom(currentProfile);
        System.out.println(result);

        if (campaignService.isCampaignComplete(currentProfile)) {
            int score = currentProfile.getCampaignScore();
            System.out.printf("Campaign complete! Final score: %,d%n", score);
            profileService.saveProfile(currentProfile);
        }
        return result;
    }

    /**
     * Generates the next campaign encounter without auto-resolving combat.
     *
     * Presentation layers can use this to run manual turn-by-turn PvE battles.
     */
    public CampaignEncounter createCampaignEncounter() {
        requireProfile();
        if (!(campaignService instanceof CampaignServiceImpl campaignImpl)) {
            throw new IllegalStateException("Interactive campaign encounters require CampaignServiceImpl.");
        }
        return campaignImpl.createEncounter(currentProfile);
    }

    /**
     * Resolves a previously generated campaign encounter.
     *
     * @param encounter generated encounter
     * @param battleWon player battle outcome (ignored for INN encounters)
     * @return resolved room result with rewards/penalties applied
     */
    public CampaignResult resolveCampaignEncounter(CampaignEncounter encounter, boolean battleWon) {
        requireProfile();
        if (!(campaignService instanceof CampaignServiceImpl campaignImpl)) {
            throw new IllegalStateException("Interactive campaign encounters require CampaignServiceImpl.");
        }

        CampaignResult result = campaignImpl.resolveEncounter(currentProfile, encounter, battleWon);
        System.out.println(result);

        if (campaignService.isCampaignComplete(currentProfile)) {
            int score = currentProfile.getCampaignScore();
            System.out.printf("Campaign complete! Final score: %,d%n", score);
            profileService.saveProfile(currentProfile);
        }

        return result;
    }

    public boolean isCampaignComplete() {
        requireProfile();
        return campaignService.isCampaignComplete(currentProfile);
    }

    // -------------------------------------------------------------------------
    // Inn
    // -------------------------------------------------------------------------

    /** Triggers a manual inn visit (revives + restores the whole party). */
    public void visitInn() {
        requireProfile();
        innService.visitInn(currentProfile);
        System.out.println("The party rests at the inn and is fully restored.");
    }

    /**
     * Purchases an inn item for the specified hero.
     *
     * @param item       the item to buy
     * @param targetHero the benefiting hero
     * @return true on success, false if the player can't afford it
     */
    public boolean buyInnItem(InnItem item, Hero targetHero) {
        requireProfile();
        boolean ok = innService.purchaseItem(currentProfile, item, targetHero);
        if (ok) {
            // Track campaign item spending for final score when using the default impl.
            if (campaignService instanceof CampaignServiceImpl campaignImpl) {
                campaignImpl.recordItemPurchase(currentProfile, item);
            }
            if (targetHero == null) {
                // Add to player's inventory instead of applying immediately
                currentProfile.addInventoryItem(item);
                System.out.printf("Purchased %s and added to inventory.%n", item.getDisplayName());
            } else {
                System.out.printf("Purchased %s for %s.%n", item.getDisplayName(), targetHero.getName());
            }
        } else {
            System.out.printf("Not enough gold to buy %s (need %dg, have %dg).%n",
                    item.getDisplayName(), item.getCost(), currentProfile.getGold());
        }
        return ok;
    }

    /**
     * Returns recruitable heroes at the current room's inn.
     */
    public List<Hero> getRecruitableHeroes() {
        requireProfile();
        return innService.getRecruitableHeroes(currentProfile, currentProfile.getCampaignRoom() + 1);
    }

    /**
     * Recruits a hero candidate into the active party.
     *
     * @param candidate a hero from {@link #getRecruitableHeroes()}
     * @return true on success
     */
    public boolean recruitHero(Hero candidate) {
        requireProfile();
        boolean ok = innService.recruitHero(currentProfile, candidate);
        if (ok) {
            candidate.addObserver(new ConsoleLogObserver());
            System.out.printf("Recruited %s (Level %d %s).%n",
                    candidate.getName(), candidate.getLevel(), candidate.getHeroClass());
            // Persist profile so recruited hero is saved across restarts
            profileService.saveProfile(currentProfile);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // PvP
    // -------------------------------------------------------------------------

    /**
     * Runs a PvP battle between two named players' saved parties.
     *
     * @param playerAName name of the first player
     * @param partyAIndex which of playerA's saved parties (0-based)
     * @param playerBName name of the second player
     * @param partyBIndex which of playerB's saved parties (0-based)
     * @return the BattleResult
     */
    public BattleResult runPvpBattle(String playerAName, int partyAIndex,
                                     String playerBName, int partyBIndex) {
        Profile profileA = profileService.loadProfile(playerAName);
        Profile profileB = profileService.loadProfile(playerBName);

        if (profileA == null || profileB == null) {
            throw new IllegalArgumentException("One or both player profiles not found.");
        }

        List<Hero> partyA = profileA.getSavedParty(partyAIndex);
        List<Hero> partyB = profileB.getSavedParty(partyBIndex);

        if (partyA == null || partyB == null) {
            throw new IllegalArgumentException("One or both party indices are invalid.");
        }

        // Ensure both sides start the PvP fight fully healthy (revived, not stunned,
        // no active shield) so the battle system runs deterministically.
        for (Hero h : partyA) {
            if (h != null) {
                h.revive();
                h.setStunned(false);
                h.setShieldAmount(0);
            }
        }
        for (Hero h : partyB) {
            if (h != null) {
                h.revive();
                h.setStunned(false);
                h.setShieldAmount(0);
            }
        }

        BattleResult result = battleService.startBattle(partyA, partyB);

        // Record league result (no exp or gold awarded in PvP)
        if (!result.isDraw()) {
            boolean aWon = result.getWinningTeam().stream().anyMatch(h -> partyA.contains(h));
            if (aWon) {
                leagueService.recordResult(playerAName, playerBName);
                profileA.addPvpWin();
                profileB.addPvpLoss();
            } else {
                leagueService.recordResult(playerBName, playerAName);
                profileB.addPvpWin();
                profileA.addPvpLoss();
            }
        }

        profileService.saveProfile(profileA);
        profileService.saveProfile(profileB);

        return result;
    }

    /**
     * Records a PvP BattleResult (used when the UI runs an interactive/manual PvP
     * session and the controller only needs to persist the outcome and update
     * league stats rather than re-running the battle logic).
     */
    public void recordPvpResult(BattleResult result,
                                String playerAName, String playerBName,
                                List<Hero> partyA, List<Hero> partyB) {
        Profile profileA = profileService.loadProfile(playerAName);
        Profile profileB = profileService.loadProfile(playerBName);

        if (profileA == null || profileB == null) {
            throw new IllegalArgumentException("One or both player profiles not found.");
        }

        if (!result.isDraw()) {
            boolean aWon = result.getWinningTeam().stream().anyMatch(p -> partyA.contains(p));
            if (aWon) {
                leagueService.recordResult(playerAName, playerBName);
                profileA.addPvpWin();
                profileB.addPvpLoss();
            } else {
                leagueService.recordResult(playerBName, playerAName);
                profileB.addPvpWin();
                profileA.addPvpLoss();
            }
        }

        profileService.saveProfile(profileA);
        profileService.saveProfile(profileB);
    }

    /**
     * Runs a battle between the active profile's active party and a provided enemy list.
     * Useful for deterministic demo rooms where enemies are hardcoded by the UI.
     *
     * @param enemies list of enemy heroes
     * @return the BattleResult
     */
    public BattleResult runCustomBattle(List<Hero> enemies) {
        requireProfile();
        BattleResult result = battleService.startBattle(currentProfile.getActiveParty(), enemies);

        // Apply rewards/penalties similar to campaign handling for demo convenience
        if (!result.isDraw() && result.getWinningTeam() == currentProfile.getActiveParty()) {
            int totalExp = enemies.stream().mapToInt(h -> 50 * h.getLevel()).sum();
            int totalGold = enemies.stream().mapToInt(h -> 75 * h.getLevel()).sum();
            var survivors = currentProfile.getActiveParty().stream().filter(Hero::isAlive).toList();
            if (!survivors.isEmpty()) {
                int expPer = totalExp / survivors.size();
                survivors.forEach(h -> h.gainExperience(expPer));
            }
            currentProfile.addGold(totalGold);
            profileService.saveProfile(currentProfile);
        } else if (!result.isDraw()) {
            // lost
            currentProfile.applyGoldPenalty();
            profileService.saveProfile(currentProfile);
        }

        return result;
    }

    /** Returns the full PvP league table. */
    public List<LeagueEntry> getLeagueTable() {
        return leagueService.getLeagueTable();
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    /** Saves the current profile. */
    public void save() {
        requireProfile();
        profileService.saveProfile(currentProfile);
        System.out.println("Profile saved.");
    }

    /** Returns the hall-of-fame (all profiles sorted by high score). */
    public List<Profile> getHallOfFame() {
        return profileService.getHallOfFame();
    }

    /** Logs out the current profile (clears active session). */
    public void logout() {
        this.currentProfile = null;
    }

    /**
     * Exit (pause) the currently active campaign and persist the profile.
     * Allowed only when a campaign is active and not during a battle.
     */
    public void exitCampaign() {
        requireProfile();
        if (!currentProfile.isCampaignActive()) {
            throw new IllegalStateException("No active campaign to exit.");
        }
        currentProfile.pauseCampaign();
        profileService.saveProfile(currentProfile);
    }

    /**
     * Resume a previously-paused campaign (does not reset progress).
     */
    public void resumeCampaign() {
        requireProfile();
        currentProfile.resumeCampaign();
        profileService.saveProfile(currentProfile);
        System.out.println("Campaign resumed.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void requireProfile() {
        if (currentProfile == null) {
            throw new IllegalStateException("No active profile. Call createProfile() or loadProfile() first.");
        }
    }
}
