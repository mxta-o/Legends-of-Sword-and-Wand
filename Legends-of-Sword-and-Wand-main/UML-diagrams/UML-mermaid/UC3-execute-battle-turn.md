# UC3 — Execute One Battle Turn

## Description
This use case describes the sequence of events when `BattleServiceImpl` executes a single turn for the acting team against the opposing team. At the start of each round, status effects are ticked for all heroes on both teams. Each team is then sorted by initiative (highest level first, ties broken by highest attack). For every alive, non-stunned hero, the AI (`decideAction`) chooses one of four actions: ATTACK, DEFEND, CAST, or WAIT. ATTACK deals damage using the formula `max(0, attacker.getCurrentAttack() − defender.getCurrentDefense())`; DEFEND restores +10 HP and +5 mana; CAST spends mana and invokes an Ability; WAIT defers the hero to a FIFO queue that acts after all normal heroes. Stunned heroes are skipped for that turn (their stun flag is consumed by the status-effect tick). The turn ends early if the opposing team is wiped out mid-turn.

```mermaid
sequenceDiagram
    participant BattleServiceImpl
    participant Hero as ActingHero
    participant Target as TargetHero
    participant Ability

    Note over BattleServiceImpl: takeTurn(actingTeam, opposingTeam)

    BattleServiceImpl->>BattleServiceImpl: tickStatusEffects(actingTeam)
    BattleServiceImpl->>BattleServiceImpl: tickStatusEffects(opposingTeam)
    BattleServiceImpl->>BattleServiceImpl: sortByInitiative(actingTeam)

    loop for each alive, non-stunned Hero in actingTeam
        BattleServiceImpl->>BattleServiceImpl: decideAction(hero, opposingTeam)
        activate BattleServiceImpl

        alt hero has castable ability (mana ≥ manaCost)
            BattleServiceImpl-->>BattleServiceImpl: Action.CAST
        else hero HP < 25%
            BattleServiceImpl-->>BattleServiceImpl: Action.DEFEND
        else
            BattleServiceImpl-->>BattleServiceImpl: Action.ATTACK
        end
        deactivate BattleServiceImpl

        alt action == ATTACK
            BattleServiceImpl->>Hero: attack(target)
            activate Hero
            Hero->>Hero: damage = max(0, getCurrentAttack() - target.getCurrentDefense())
            Hero->>Target: takeDamage(damage)
            activate Target
            Target->>Target: shieldAmount absorbs first, then currentHealth -= damage
            alt currentHealth <= 0
                Target->>Target: isAlive = false
            end
            deactivate Target
            deactivate Hero

        else action == DEFEND
            BattleServiceImpl->>Hero: defend()
            activate Hero
            Hero->>Hero: heal(10)
            Hero->>Hero: restoreMana(5)
            deactivate Hero

        else action == CAST
            BattleServiceImpl->>BattleServiceImpl: performCast(hero, allies, enemies)
            BattleServiceImpl->>Hero: canCast(ability)
            Hero-->>BattleServiceImpl: true
            BattleServiceImpl->>Hero: castAbility(ability, targets)
            activate Hero
            Hero->>Hero: currentMana -= ability.getManaCost()
            Hero->>Ability: execute(caster, targets)
            activate Ability
            Ability-->>Hero: ability effect applied
            deactivate Ability
            deactivate Hero

        else action == WAIT
            BattleServiceImpl->>Hero: waitTurn()
            BattleServiceImpl->>BattleServiceImpl: waitQueue.add(hero)
        end

        alt opposingTeam all dead
            BattleServiceImpl->>BattleServiceImpl: break (early exit)
        end
    end

    loop while waitQueue not empty (FIFO)
        BattleServiceImpl->>Hero: attack(target)
        Note over BattleServiceImpl,Hero: Deferred heroes default to ATTACK
    end
```
