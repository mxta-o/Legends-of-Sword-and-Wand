package model;

import java.time.Instant;

/**
 * Lightweight representation of a persisted PvP match/invite.
 */
public class PvpMatch {

    public enum Status { PENDING, ACTIVE, COMPLETED }

    private long id;
    private String playerA;
    private String playerB;
    private int partyAIndex;
    private int partyBIndex;
    private String currentTurn; // player name who has the current turn
    private Status status;
    private String state; // optional opaque serialized game state
    private Instant createdAt;
    private Instant updatedAt;

    public PvpMatch() { }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPlayerA() { return playerA; }
    public void setPlayerA(String playerA) { this.playerA = playerA; }

    public String getPlayerB() { return playerB; }
    public void setPlayerB(String playerB) { this.playerB = playerB; }

    public int getPartyAIndex() { return partyAIndex; }
    public void setPartyAIndex(int partyAIndex) { this.partyAIndex = partyAIndex; }

    public int getPartyBIndex() { return partyBIndex; }
    public void setPartyBIndex(int partyBIndex) { this.partyBIndex = partyBIndex; }

    public String getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(String currentTurn) { this.currentTurn = currentTurn; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
