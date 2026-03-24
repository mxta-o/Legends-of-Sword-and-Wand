package model;

import java.util.List;

/**
 * Represents the result of a battle, including the outcome and final state of teams.
 */
public class BattleResult {
    private List<Hero> winningTeam;
    private List<Hero> losingTeam;
    private boolean isDraw;
    // Add more fields as needed (e.g., turn log, stats)

    public BattleResult() {}

    public BattleResult(List<Hero> winningTeam, List<Hero> losingTeam, boolean isDraw) {
        this.winningTeam = winningTeam;
        this.losingTeam = losingTeam;
        this.isDraw = isDraw;
    }

    public List<Hero> getWinningTeam() {
        return winningTeam;
    }

    public List<Hero> getLosingTeam() {
        return losingTeam;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public void setWinningTeam(List<Hero> winningTeam) {
        this.winningTeam = winningTeam;
    }

    public void setLosingTeam(List<Hero> losingTeam) {
        this.losingTeam = losingTeam;
    }

    public void setDraw(boolean draw) {
        isDraw = draw;
    }
}
