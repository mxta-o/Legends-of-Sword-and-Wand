package persistence;

import model.PvpMatch;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed implementation for match persistence (invites + matches).
 */
public class SQLiteMatchRepository implements MatchRepository {

    private final String jdbcUrl;

    public SQLiteMatchRepository(String dbFilePath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
        initSchema();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initSchema() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pvp_invites (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_player TEXT NOT NULL,
                    to_player   TEXT NOT NULL,
                    created_at  INTEGER NOT NULL
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pvp_matches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_a TEXT NOT NULL,
                    player_b TEXT NOT NULL,
                    party_a_index INTEGER NOT NULL,
                    party_b_index INTEGER NOT NULL,
                    current_turn TEXT,
                    status TEXT NOT NULL,
                    state TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )""");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise match schema", e);
        }
    }

    @Override
    public boolean insertInvite(String fromPlayer, String toPlayer) {
        String sql = "INSERT INTO pvp_invites (from_player, to_player, created_at) VALUES (?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromPlayer);
            ps.setString(2, toPlayer);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("insertInvite failed", e);
        }
    }

    @Override
    public List<String> pollInvitesFor(String toPlayer) {
        String select = "SELECT id, from_player FROM pvp_invites WHERE to_player=? ORDER BY created_at";
        String delete = "DELETE FROM pvp_invites WHERE id=?";
        List<String> out = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, toPlayer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String from = rs.getString("from_player");
                    out.add(from);
                    try (PreparedStatement del = conn.prepareStatement(delete)) {
                        del.setLong(1, id);
                        del.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("pollInvitesFor failed", e);
        }
        return out;
    }

    @Override
    public PvpMatch createMatch(PvpMatch match) {
        String sql = "INSERT INTO pvp_matches (player_a, player_b, party_a_index, party_b_index, current_turn, status, state, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, match.getPlayerA());
            ps.setString(2, match.getPlayerB());
            ps.setInt(3, match.getPartyAIndex());
            ps.setInt(4, match.getPartyBIndex());
            ps.setString(5, match.getCurrentTurn());
            ps.setString(6, match.getStatus().name());
            ps.setString(7, match.getState());
            ps.setLong(8, match.getCreatedAt() == null ? Instant.now().toEpochMilli() : match.getCreatedAt().toEpochMilli());
            ps.setLong(9, match.getUpdatedAt() == null ? Instant.now().toEpochMilli() : match.getUpdatedAt().toEpochMilli());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    match.setId(rs.getLong(1));
                }
            }
            return match;
        } catch (SQLException e) {
            throw new RuntimeException("createMatch failed", e);
        }
    }

    @Override
    public void updateMatch(PvpMatch match) {
        String sql = "UPDATE pvp_matches SET current_turn=?, status=?, state=?, updated_at=? WHERE id=?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, match.getCurrentTurn());
            ps.setString(2, match.getStatus().name());
            ps.setString(3, match.getState());
            ps.setLong(4, Instant.now().toEpochMilli());
            ps.setLong(5, match.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateMatch failed", e);
        }
    }

    @Override
    public PvpMatch findById(long id) {
        String sql = "SELECT * FROM pvp_matches WHERE id=?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    @Override
    public List<PvpMatch> findMatchesForPlayer(String playerName) {
        String sql = "SELECT * FROM pvp_matches WHERE player_a=? OR player_b=? ORDER BY updated_at DESC";
        List<PvpMatch> out = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findMatchesForPlayer failed", e);
        }
        return out;
    }

    private PvpMatch mapRow(ResultSet rs) throws SQLException {
        PvpMatch m = new PvpMatch();
        m.setId(rs.getLong("id"));
        m.setPlayerA(rs.getString("player_a"));
        m.setPlayerB(rs.getString("player_b"));
        m.setPartyAIndex(rs.getInt("party_a_index"));
        m.setPartyBIndex(rs.getInt("party_b_index"));
        m.setCurrentTurn(rs.getString("current_turn"));
        m.setStatus(PvpMatch.Status.valueOf(rs.getString("status")));
        m.setState(rs.getString("state"));
        m.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        m.setUpdatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));
        return m;
    }
}
