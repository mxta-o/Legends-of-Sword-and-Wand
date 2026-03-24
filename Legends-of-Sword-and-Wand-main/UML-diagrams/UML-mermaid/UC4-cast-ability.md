# UC4 — Cast a Special Ability

## Description
This use case describes the sequence when a hero casts a special ability during their turn. The battle engine calls `canCast(ability)` to verify the hero has sufficient mana, then calls `castAbility(ability, targets)`. The hero deducts the mana cost and delegates execution to the `Ability` object by calling `execute(caster, targets)`. The concrete ability subclass (Fireball, ChainLightning, Protect, Heal, BerserkerAttack, or Replenish) then applies its effect—dealing damage, applying status effects, granting shields, or restoring resources—to one or more targets. The diagram covers the three primary ability archetypes: single-target damage (Fireball), multi-target damage (ChainLightning), and self/ally support (Heal / Protect / Replenish).

```mermaid
sequenceDiagram
    participant BattleServiceImpl
    participant Hero as CasterHero
    participant Ability
    participant Target1 as Target (Primary)
    participant Target2 as Target (Secondary)

    BattleServiceImpl->>Hero: canCast(ability)
    activate Hero
    Hero-->>BattleServiceImpl: currentMana >= ability.getManaCost()
    deactivate Hero

    BattleServiceImpl->>Hero: castAbility(ability, targets)
    activate Hero

    Hero->>Hero: currentMana -= ability.getManaCost()
    Hero->>Ability: execute(caster, targets)
    activate Ability

    alt ability is Fireball (single-target damage)
        Ability->>Target1: takeDamage(power - target.getCurrentDefense())
        Note over Ability,Target1: damage = max(0, power - defense)

    else ability is ChainLightning (multi-target damage)
        Ability->>Target1: takeDamage(power - target.getCurrentDefense())
        Ability->>Target2: takeDamage((power/2) - target.getCurrentDefense())
        Note over Ability,Target2: Secondary hits deal half power

    else ability is BerserkerAttack (self-buff + damage)
        Ability->>Hero: addBaseAttack(+5) [temporary buff]
        Ability->>Target1: takeDamage(boosted damage)
        Ability->>Hero: addBaseAttack(-5) [revert buff]

    else ability is Protect (self shield)
        Ability->>Hero: addShield(amount)
        Note over Ability,Hero: shieldAmount absorbs future damage

    else ability is Heal (self restore HP)
        Ability->>Hero: heal(amount)
        Note over Ability,Hero: currentHealth = min(current + amount, maxHealth)

    else ability is Replenish (self restore mana)
        Ability->>Hero: restoreMana(amount)
        Note over Ability,Hero: currentMana = min(current + amount, maxMana)
    end

    Ability-->>Hero: effect applied
    deactivate Ability

    Hero-->>BattleServiceImpl: (turn complete)
    deactivate Hero
```
