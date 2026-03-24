# UC5 — Full Battle

## Description
This use case describes the complete lifecycle of a team vs. team battle from start to finish. The caller (game controller) invokes `startBattle(teamA, teamB)` on `BattleServiceImpl`. The service creates working copies of both teams so the originals remain unmodified, then enters the main battle loop. Each iteration: (1) status effects are ticked for every hero on both teams, (2) each team is sorted by initiative, (3) teamA heroes take their turns against teamB, then (4) teamB heroes take their turns against teamA. After both sets of turns the round counter increments; a draw is declared if 1,000 rounds elapse without a winner. The loop exits when one team has no alive heroes. The service then constructs a `BattleResult` recording the winning team, losing team, and draw flag, and returns it to the caller.

```mermaid
sequenceDiagram
    actor GameController
    participant BattleServiceImpl
    participant TeamA as TeamA Heroes
    participant TeamB as TeamB Heroes
    participant BattleResult

    GameController->>BattleServiceImpl: startBattle(teamA, teamB)
    activate BattleServiceImpl

    BattleServiceImpl->>BattleServiceImpl: teamACopy = new ArrayList(teamA)
    BattleServiceImpl->>BattleServiceImpl: teamBCopy = new ArrayList(teamB)
    Note over BattleServiceImpl: turn = 0, isDraw = false

    loop while teamA has alive heroes AND teamB has alive heroes

        BattleServiceImpl->>TeamA: processStatusEffects() [for each hero]
        BattleServiceImpl->>TeamB: processStatusEffects() [for each hero]

        BattleServiceImpl->>BattleServiceImpl: sortByInitiative(teamACopy)
        BattleServiceImpl->>BattleServiceImpl: sortByInitiative(teamBCopy)

        Note over BattleServiceImpl: takeTurn(teamACopy, teamBCopy)
        BattleServiceImpl->>TeamA: decide & execute each hero's action vs TeamB
        activate TeamA
        TeamA->>TeamB: attack / cast / defend / wait
        deactivate TeamA

        alt teamB fully defeated mid-turn
            BattleServiceImpl->>BattleServiceImpl: break early
        end

        Note over BattleServiceImpl: takeTurn(teamBCopy, teamACopy)
        BattleServiceImpl->>TeamB: decide & execute each hero's action vs TeamA
        activate TeamB
        TeamB->>TeamA: attack / cast / defend / wait
        deactivate TeamB

        alt teamA fully defeated mid-turn
            BattleServiceImpl->>BattleServiceImpl: break early
        end

        BattleServiceImpl->>BattleServiceImpl: turn++

        alt turn > 1000
            BattleServiceImpl->>BattleServiceImpl: isDraw = true
            BattleServiceImpl->>BattleServiceImpl: break
        end
    end

    alt isDraw
        BattleServiceImpl->>BattleResult: new BattleResult([], [], isDraw=true)
    else teamA alive
        BattleServiceImpl->>BattleResult: new BattleResult(teamACopy, teamBCopy, false)
    else teamB alive
        BattleServiceImpl->>BattleResult: new BattleResult(teamBCopy, teamACopy, false)
    end
    activate BattleResult

    BattleResult-->>BattleServiceImpl: result
    deactivate BattleResult

    BattleServiceImpl-->>GameController: BattleResult (winner, loser, isDraw)
    deactivate BattleServiceImpl
```
