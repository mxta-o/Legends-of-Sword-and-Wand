package model;

/**
 * One row in the PvP league table.
 */
public class LeagueEntry implements Comparable<LeagueEntry> {

    private final String playerName;
    private int wins;
    private int losses;

    public LeagueEntry(String playerName) {
        this.playerName = playerName;
        this.wins       = 0;
        this.losses     = 0;
    }

    public String getPlayerName() { return playerName; }
    public int    getWins()       { return wins; }
    public int    getLosses()     { return losses; }

    public void recordWin()  { wins++; }
    public void recordLoss() { losses++; }

    public int getTotalGames() { return wins + losses; }

    /** Win-rate as a double in [0,1]. Returns 0 if no games played. */
    public double getWinRate() {
        return getTotalGames() == 0 ? 0.0 : (double) wins / getTotalGames();
    }

    /** Sort by wins desc, then losses asc. */
    @Override
    public int compareTo(LeagueEntry other) {
        if (this.wins != other.wins) return Integer.compare(other.wins, this.wins);
        return Integer.compare(this.losses, other.losses);
    }

    @Override
    public String toString() {
        return String.format("%-20s W:%3d L:%3d (%.0f%%)",
                playerName, wins, losses, getWinRate() * 100);
    }
}
