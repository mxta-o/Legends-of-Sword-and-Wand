# UC6 — PvE Campaign Room Encounter

## Description
This use case describes what happens when the player enters a room during the PvE campaign. The campaign controller evaluates the cumulative level of the player's party to determine the encounter probabilities (base: 60% battle / 40% inn, shifting +3% toward battle for every 10 cumulative hero levels, capped at 90%/10%). A random roll decides the room type. If the room is a **battle**, a scaled enemy party (1–5 units, levels 1–10 scaled to the player's cumulative level) is generated and `startBattle()` is called. On victory the player earns experience (`Exp(L) = 50*L` per enemy unit, split among surviving heroes) and gold (`G(L) = 75*L` per enemy); on defeat, the player loses 10% gold and 30% of current-level experience and is returned to the last inn. If the room is an **inn**, the inn visit use case (UC7) is triggered. After 30 rooms the campaign ends and a final score is computed (100 pts per hero level + 10 pts per gold coin + 0.5× item purchase price × 10 for each item bought).

```mermaid
sequenceDiagram
    actor Player
    participant CampaignController
    participant RoomGenerator
    participant BattleServiceImpl
    participant InnService
    participant Hero

    Player->>CampaignController: enterNextRoom()
    activate CampaignController

    CampaignController->>CampaignController: cumulativeLevel = sum of party hero levels
    CampaignController->>CampaignController: battleChance = min(90, 60 + 3*(cumulativeLevel/10))
    CampaignController->>RoomGenerator: rollRoomType(battleChance)
    activate RoomGenerator
    RoomGenerator-->>CampaignController: roomType (BATTLE | INN)
    deactivate RoomGenerator

    alt roomType == BATTLE
        CampaignController->>RoomGenerator: generateEnemyParty(cumulativeLevel)
        activate RoomGenerator
        RoomGenerator-->>CampaignController: enemyParty (1–5 heroes, scaled levels)
        deactivate RoomGenerator

        CampaignController->>BattleServiceImpl: startBattle(playerParty, enemyParty)
        activate BattleServiceImpl
        BattleServiceImpl-->>CampaignController: BattleResult (winner, loser, isDraw)
        deactivate BattleServiceImpl

        alt BattleResult: player wins
            loop for each defeated enemy unit
                CampaignController->>CampaignController: exp += 50 * enemyLevel
                CampaignController->>CampaignController: gold += 75 * enemyLevel
            end
            CampaignController->>CampaignController: distribute exp among surviving heroes
            loop for each surviving Hero
                CampaignController->>Hero: gainExperience(shareOfExp)
            end
            CampaignController-->>Player: victory — exp & gold awarded
        else BattleResult: player loses
            CampaignController->>CampaignController: gold -= gold * 0.10
            loop for each Hero
                CampaignController->>Hero: loseExperience(currentLevelExp * 0.30)
            end
            CampaignController->>CampaignController: teleportToLastInn()
            CampaignController-->>Player: defeat — gold & exp penalty, returned to inn
        end

    else roomType == INN
        CampaignController->>InnService: visitInn(playerParty)
        activate InnService
        InnService-->>CampaignController: inn actions complete
        deactivate InnService
        CampaignController-->>Player: inn visit complete
    end

    CampaignController->>CampaignController: roomCount++

    alt roomCount == 30
        CampaignController->>CampaignController: calculateFinalScore()
        Note over CampaignController: score = Σ(heroLevel * 100)<br/>+ gold * 10<br/>+ Σ(itemBuyPrice/2 * 10)
        CampaignController-->>Player: campaign complete — final score
    end

    deactivate CampaignController
```
