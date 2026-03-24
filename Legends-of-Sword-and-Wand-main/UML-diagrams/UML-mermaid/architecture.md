# System Architecture — Legends of Sword and Wand

## Block Diagram

```mermaid
graph TD
    subgraph Presentation["Presentation Layer (Person B)"]
        UI[Game UI / Console]
        PvECtrl[CampaignController]
        PvPCtrl[PvPController]
        InnCtrl[InnService]
    end

    subgraph Service["Service Layer"]
        BS[BattleService interface]
        BSI[BattleServiceImpl]
        LS[LeagueService]
    end

    subgraph Domain["Domain / Model Layer"]
        Hero[Hero]
        HC[HeroClass enum]
        HCS[HeroClassStrategy interface]
        OS[OrderStrategy]
        CS[ChaosStrategy]
        WS[WarriorStrategy]
        MS[MageStrategy]
        AB[Ability abstract]
        FB[Fireball]
        CL[ChainLightning]
        BA[BerserkerAttack]
        PT[Protect]
        HL[Heal]
        RP[Replenish]
        SE[StatusEffect]
        ST[StatusType enum]
        BR[BattleResult]
    end

    subgraph Persistence["Persistence Layer (Person B)"]
        DB[(Database)]
        ProfileRepo[ProfileRepository]
        PartyRepo[PartyRepository]
    end

    UI --> PvECtrl
    UI --> PvPCtrl
    UI --> InnCtrl
    PvECtrl --> BS
    PvPCtrl --> BS
    BS --> BSI
    BSI --> Hero
    BSI --> BR
    InnCtrl --> Hero
    Hero --> HCS
    HCS --> OS
    HCS --> CS
    HCS --> WS
    HCS --> MS
    Hero --> AB
    AB --> FB
    AB --> CL
    AB --> BA
    AB --> PT
    AB --> HL
    AB --> RP
    Hero --> SE
    SE --> ST
    PvECtrl --> ProfileRepo
    PvECtrl --> PartyRepo
    ProfileRepo --> DB
    PartyRepo --> DB
    BSI --> LS
```

## Module Descriptions

| Module | Description |
|--------|-------------|
| **Domain / Model** | Core game entities: `Hero`, `HeroClass`, `Ability` hierarchy, `StatusEffect`, `BattleResult`. Contains all business rules. |
| **Service** | Orchestrates battles via `BattleService` / `BattleServiceImpl`. Stateless; delegates all business rules to domain objects. |
| **Presentation** | Controllers for PvE campaign, PvP, and inn. Drives game flow (Person B). |
| **Persistence** | Database repositories for hero profiles and party state (Person B). |
