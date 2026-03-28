package persistence;

import model.Hero;
import model.HeroClass;
import model.Profile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed implementation of ProfileRepository (DP6 — Repository / DAO).
 *
 * Schema (auto-created on first use):
 *
 *   profiles(player_name TEXT PK, gold INTEGER, pvp_wins INTEGER,
 *            pvp_losses INTEGER, campaign_room INTEGER,
 *            campaign_active INTEGER, high_score INTEGER)
 *
 *   heroes(id INTEGER PK, player_name TEXT FK, hero_name TEXT,
 *          hero_class TEXT, level INTEGER, experience INTEGER,
 *          current_health INTEGER, current_mana INTEGER,
 *          base_attack INTEGER, base_defense INTEGER,
 *          is_alive INTEGER, party_index INTEGER)
 *
 * Usage:
 *   ProfileRepository repo = new SQLiteProfileRepository("game.db");
 *
 * Requires the SQLite JDBC driver on the classpath:
 *   org.xerial:sqlite-jdbc:3.45.x (add to pom.xml for Deliverable 3)
 */
public class SQLiteProfileRepository implements ProfileRepository {

    private final String jdbcUrl;

    public SQLiteProfileRepository(String dbFilePath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
        initSchema();
    }

    // -------------------------------------------------------------------------
    // Schema initialisation
    // -------------------------------------------------------------------------

    private void initSchema() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS profiles (
                        player_name    TEXT PRIMARY KEY,
                        gold           INTEGER NOT NULL DEFAULT 0,
                        pvp_wins       INTEGER NOT NULL DEFAULT 0,
                        pvp_losses     INTEGER NOT NULL DEFAULT 0,
                        campaign_room  INTEGER NOT NULL DEFAULT 0,
                        campaign_active INTEGER NOT NULL DEFAULT 0,
                        high_score     INTEGER NOT NULL DEFAULT 0,
                        password_hash  TEXT,
                        password_salt  TEXT
                    )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS heroes (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name    TEXT NOT NULL,
                    hero_name      TEXT NOT NULL,
                    hero_class     TEXT NOT NULL,
                    level          INTEGER NOT NULL DEFAULT 1,
                    experience     INTEGER NOT NULL DEFAULT 0,
                    current_health INTEGER NOT NULL DEFAULT 100,
                    current_mana   INTEGER NOT NULL DEFAULT 50,
                    base_attack    INTEGER NOT NULL DEFAULT 5,
                    base_defense   INTEGER NOT NULL DEFAULT 5,
                    is_alive       INTEGER NOT NULL DEFAULT 1,
                    party_index    INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (player_name) REFERENCES profiles(player_name) ON DELETE CASCADE
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS saved_parties (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name   TEXT NOT NULL,
                    slot_index    INTEGER NOT NULL,
                    FOREIGN KEY (player_name) REFERENCES profiles(player_name) ON DELETE CASCADE
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS saved_party_heroes (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    saved_party_id   INTEGER NOT NULL,
                    hero_index       INTEGER NOT NULL,
                    hero_name        TEXT NOT NULL,
                    hero_class       TEXT NOT NULL,
                    level            INTEGER NOT NULL DEFAULT 1,
                    experience       INTEGER NOT NULL DEFAULT 0,
                    current_health   INTEGER NOT NULL DEFAULT 100,
                    current_mana     INTEGER NOT NULL DEFAULT 50,
                    base_attack      INTEGER NOT NULL DEFAULT 5,
                    base_defense     INTEGER NOT NULL DEFAULT 5,
                    is_alive         INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (saved_party_id) REFERENCES saved_parties(id) ON DELETE CASCADE
                )""");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise SQLite schema", e);
        }
    }

    // -------------------------------------------------------------------------
    // ProfileRepository interface
    // -------------------------------------------------------------------------

    @Override
    public void insert(Profile profile) {
        if (exists(profile.getPlayerName())) {
            throw new IllegalArgumentException(
                    "Profile already exists: " + profile.getPlayerName());
        }
            String sql = """
                INSERT INTO profiles (player_name, gold, pvp_wins, pvp_losses,
                                      campaign_room, campaign_active, high_score, password_hash, password_salt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindProfileParams(ps, profile);
            ps.executeUpdate();
            saveHeroes(conn, profile);
            saveSavedParties(conn, profile);
        } catch (SQLException e) {
            throw new RuntimeException("insert failed", e);
        }
    }

    @Override
    public void update(Profile profile) {
        String sql = """
                UPDATE profiles SET gold=?, pvp_wins=?, pvp_losses=?,
                    campaign_room=?, campaign_active=?, high_score=?, password_hash=?, password_salt=?
                WHERE player_name=?""";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,    profile.getGold());
            ps.setInt(2,    profile.getPvpWins());
            ps.setInt(3,    profile.getPvpLosses());
            ps.setInt(4,    profile.getCampaignRoom());
            ps.setInt(5,    profile.isCampaignActive() ? 1 : 0);
            ps.setInt(6,    profile.getHighScore());
            ps.setString(7, profile.getPasswordHash());
            ps.setString(8, profile.getPasswordSalt());
            ps.setString(9, profile.getPlayerName());
            ps.executeUpdate();

            // Refresh hero rows
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM heroes WHERE player_name=?")) {
                del.setString(1, profile.getPlayerName());
                del.executeUpdate();
            }
            saveHeroes(conn, profile);
            // Refresh saved parties as well
            try (PreparedStatement del2 = conn.prepareStatement(
                    "DELETE FROM saved_party_heroes WHERE saved_party_id IN (SELECT id FROM saved_parties WHERE player_name=?)")) {
                del2.setString(1, profile.getPlayerName());
                del2.executeUpdate();
            }
            try (PreparedStatement del3 = conn.prepareStatement(
                    "DELETE FROM saved_parties WHERE player_name=?")) {
                del3.setString(1, profile.getPlayerName());
                del3.executeUpdate();
            }
            saveSavedParties(conn, profile);

        } catch (SQLException e) {
            throw new RuntimeException("update failed", e);
        }
    }

    @Override
    public Profile findByName(String playerName) {
        String sql = "SELECT * FROM profiles WHERE player_name=?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Profile profile = mapRowToProfile(rs);
                loadHeroes(conn, profile);
                loadSavedParties(conn, profile);
                return profile;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByName failed", e);
        }
    }

    @Override
    public List<Profile> findAllByHighScoreDesc() {
        String sql = "SELECT * FROM profiles ORDER BY high_score DESC";
        List<Profile> result = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Profile profile = mapRowToProfile(rs);
                loadHeroes(conn, profile);
                loadSavedParties(conn, profile);
                result.add(profile);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAllByHighScoreDesc failed", e);
        }
        return result;
    }

    @Override
    public void deleteByName(String playerName) {
        String sql = "DELETE FROM profiles WHERE player_name=?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteByName failed", e);
        }
    }

    @Override
    public boolean exists(String playerName) {
        String sql = "SELECT 1 FROM profiles WHERE player_name=?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("exists check failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void bindProfileParams(PreparedStatement ps, Profile profile) throws SQLException {
        ps.setString(1, profile.getPlayerName());
        ps.setInt(2,    profile.getGold());
        ps.setInt(3,    profile.getPvpWins());
        ps.setInt(4,    profile.getPvpLosses());
        ps.setInt(5,    profile.getCampaignRoom());
        ps.setInt(6,    profile.isCampaignActive() ? 1 : 0);
        ps.setInt(7,    profile.getHighScore());
            // use getters for password hash and salt
            ps.setString(8, profile.getPasswordHash());
            ps.setString(9, profile.getPasswordSalt());
    }

    private Profile mapRowToProfile(ResultSet rs) throws SQLException {
        Profile p = new Profile(rs.getString("player_name"));
        p.addGold(rs.getInt("gold"));
        int wins   = rs.getInt("pvp_wins");
        int losses = rs.getInt("pvp_losses");
        for (int i = 0; i < wins;   i++) p.addPvpWin();
        for (int i = 0; i < losses; i++) p.addPvpLoss();
        // Restore campaign room counter
        int room = rs.getInt("campaign_room");
        if (rs.getInt("campaign_active") == 1) p.startCampaign();
        for (int i = 0; i < room; i++) p.advanceCampaignRoom();
            // restore password hash if present
            try {
                String ph = rs.getString("password_hash");
                String psalt = rs.getString("password_salt");
                if ((ph != null && !ph.isEmpty()) || (psalt != null && !psalt.isEmpty())) {
                    p.setPasswordHashAndSalt(ph, psalt);
                }
            } catch (Exception ignored) {}
        return p;
    }

    private void saveHeroes(Connection conn, Profile profile) throws SQLException {
        String sql = """
            INSERT INTO heroes (player_name, hero_name, hero_class, level, experience,
                                current_health, current_mana, base_attack, base_defense,
                                is_alive, party_index)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

        List<Hero> party = profile.getActiveParty();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < party.size(); i++) {
                Hero h = party.get(i);
                ps.setString(1, profile.getPlayerName());
                ps.setString(2, h.getName());
                ps.setString(3, h.getHeroClass().name());
                ps.setInt(4,    h.getLevel());
                ps.setInt(5,    h.getExperience());
                ps.setInt(6,    h.getCurrentHealth());
                ps.setInt(7,    h.getCurrentMana());
                ps.setInt(8,    h.getCurrentAttack());
                ps.setInt(9,    h.getCurrentDefense());
                ps.setInt(10,   h.isAlive() ? 1 : 0);
                ps.setInt(11,   i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveSavedParties(Connection conn, Profile profile) throws SQLException {
        String insertParty = "INSERT INTO saved_parties (player_name, slot_index) VALUES (?, ?)";
        String insertHero = "INSERT INTO saved_party_heroes (saved_party_id, hero_index, hero_name, hero_class, level, experience, current_health, current_mana, base_attack, base_defense, is_alive) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<List<Hero>> parties = profile.getSavedParties();
        if (parties == null || parties.isEmpty()) return;

        try (PreparedStatement psParty = conn.prepareStatement(insertParty, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psHero = conn.prepareStatement(insertHero)) {
            for (int slot = 0; slot < parties.size(); slot++) {
                List<Hero> party = parties.get(slot);
                psParty.setString(1, profile.getPlayerName());
                psParty.setInt(2, slot);
                psParty.executeUpdate();
                try (ResultSet gen = psParty.getGeneratedKeys()) {
                    if (!gen.next()) continue;
                    long savedPartyId = gen.getLong(1);
                    for (int i = 0; i < party.size(); i++) {
                        Hero h = party.get(i);
                        psHero.setLong(1, savedPartyId);
                        psHero.setInt(2, i);
                        psHero.setString(3, h.getName());
                        psHero.setString(4, h.getHeroClass().name());
                        psHero.setInt(5, h.getLevel());
                        psHero.setInt(6, h.getExperience());
                        psHero.setInt(7, h.getCurrentHealth());
                        psHero.setInt(8, h.getCurrentMana());
                        psHero.setInt(9, h.getCurrentAttack());
                        psHero.setInt(10, h.getCurrentDefense());
                        psHero.setInt(11, h.isAlive() ? 1 : 0);
                        psHero.addBatch();
                    }
                    psHero.executeBatch();
                }
            }
        }
    }

    private void loadSavedParties(Connection conn, Profile profile) throws SQLException {
        // Clear any existing saved parties in memory
        profile.getSavedParties().clear();

        String sqlParties = "SELECT id, slot_index FROM saved_parties WHERE player_name=? ORDER BY slot_index";
        String sqlHeroes = "SELECT * FROM saved_party_heroes WHERE saved_party_id=? ORDER BY hero_index";

        try (PreparedStatement psParties = conn.prepareStatement(sqlParties);
             PreparedStatement psHeroes = conn.prepareStatement(sqlHeroes)) {
            psParties.setString(1, profile.getPlayerName());
            try (ResultSet rs = psParties.executeQuery()) {
                while (rs.next()) {
                    long savedPartyId = rs.getLong("id");
                    List<Hero> party = new ArrayList<>();
                    psHeroes.setLong(1, savedPartyId);
                    try (ResultSet hrs = psHeroes.executeQuery()) {
                        while (hrs.next()) {
                            HeroClass heroClass = HeroClass.valueOf(hrs.getString("hero_class"));
                            Hero hero = new Hero(hrs.getString("hero_name"), heroClass);
                            int level = hrs.getInt("level");
                            for (int lvl = 1; lvl < level; lvl++) hero.levelUp(heroClass);
                            if (hrs.getInt("is_alive") == 0 && hero.isAlive()) {
                                hero.takeDamage(hero.getCurrentMaxHealth());
                            }
                            party.add(hero);
                        }
                    }
                    profile.getSavedParties().add(party);
                }
            }
        }
    }

    private void loadHeroes(Connection conn, Profile profile) throws SQLException {
        String sql = """
            SELECT * FROM heroes WHERE player_name=? ORDER BY party_index""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getPlayerName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HeroClass heroClass = HeroClass.valueOf(rs.getString("hero_class"));
                    Hero hero = new Hero(rs.getString("hero_name"), heroClass);
                    // Re-apply levels (we store the final level; re-levelling
                    // reconstructs base stats faithfully)
                    int level = rs.getInt("level");
                    for (int lvl = 1; lvl < level; lvl++) {
                        hero.levelUp(heroClass);
                    }
                    if (rs.getInt("is_alive") == 0 && hero.isAlive()) {
                        hero.takeDamage(hero.getCurrentMaxHealth());
                    }
                    profile.addHeroToParty(hero);
                }
            }
        }
    }
}
