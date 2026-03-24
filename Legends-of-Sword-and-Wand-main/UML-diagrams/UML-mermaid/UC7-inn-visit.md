# UC7 — Inn Visit

## Description
This use case describes what happens when the player's party visits an inn, either because the campaign room rolled an inn encounter or because the player was sent back after a defeat. On arrival, `InnService` automatically revives all fallen heroes and fully restores every hero's HP and mana at no cost. The player may then browse the shop and purchase food items (+HP) or drink items (+mana), provided they have enough gold; each purchase immediately applies the item's effect to the chosen hero and deducts the gold cost. In the first 10 rooms, one or more unemployed heroes may also be present for recruiting: level 1 heroes join for free, while higher-level heroes cost 200g per level. Recruiting adds the hero to the party (capped at 5 members). The player leaves the inn when they choose to, and the campaign continues from where they left off.

```mermaid
sequenceDiagram
    actor Player
    participant InnService
    participant Hero
    participant Party

    Player->>InnService: visitInn(party)
    activate InnService

    Note over InnService: Auto-restore on arrival (free)
    loop for each Hero in party
        InnService->>Hero: revive()
        activate Hero
        Hero->>Hero: isAlive = true
        Hero->>Hero: currentHealth = getCurrentMaxHealth()
        Hero->>Hero: currentMana  = getCurrentMaxMana()
        deactivate Hero
    end

    InnService-->>Player: all heroes fully restored

    loop Player browses shop
        Player->>InnService: purchaseItem(itemType, targetHero)
        activate InnService

        InnService->>InnService: check player.gold >= item.cost
        alt sufficient gold
            InnService->>InnService: player.gold -= item.cost

            alt item is food (Bread/Cheese/Steak)
                InnService->>Hero: heal(item.hpAmount)
                Note over Hero: +20 / +50 / +200 HP (capped at maxHealth)
            else item is drink (Water/Juice/Wine)
                InnService->>Hero: restoreMana(item.manaAmount)
                Note over Hero: +10 / +30 / +100 mana (capped at maxMana)
            else item is Elixir
                InnService->>Hero: revive()
                Note over Hero: Full HP + Full mana + isAlive = true
            end

            InnService-->>Player: purchase successful, effect applied
        else insufficient gold
            InnService-->>Player: purchase failed — not enough gold
        end
        deactivate InnService
    end

    alt roomNumber <= 10 AND party.size() < 5
        Note over InnService: Unemployed heroes available for recruiting
        InnService-->>Player: show available recruits (random class, level 1–4)

        Player->>InnService: recruitHero(recruitHero)
        activate InnService

        InnService->>InnService: check recruit.level == 1 OR player.gold >= recruit.level * 200
        alt free recruit (level 1)
            InnService->>Party: addHero(recruitHero)
            InnService-->>Player: hero recruited for free
        else paid recruit
            InnService->>InnService: player.gold -= recruit.level * 200
            InnService->>Party: addHero(recruitHero)
            InnService-->>Player: hero recruited — gold deducted
        end
        deactivate InnService
    end

    Player->>InnService: leaveInn()
    InnService-->>Player: inn visit complete
    deactivate InnService
```
