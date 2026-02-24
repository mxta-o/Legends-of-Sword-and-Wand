classDiagram

%% =========================
%% HERO
%% =========================

class Hero {
    -String name
    -int level
    -int baseAttack
    -int baseDefense
    -int maxHealth
    -int currentHealth
    -int maxMana
    -int currentMana
    -Map~ClassType, Integer~ classLevels
    -HeroClass heroClass
    -List~StatusEffect~ statusEffects

    +levelUp(classType: ClassType) void
    +takeDamage(amount: int) void
    +heal(amount: int) void
    +restoreMana(amount: int) void
    +isAlive() boolean
    +attack(target: Hero) void
    +defend() void
    +waitTurn() void
    +castAbility(ability: Ability, targets: List~Hero~) void
}

%% =========================
%% HERO CLASS (STRATEGY)
%% =========================

class HeroClass {
    <<interface>>
    +applyLevelBonus(hero: Hero) void
    +getAbilities() List~Ability~
    +getClassName() String
}

class Order
class Chaos
class Warrior
class Mage

HeroClass <|.. Order
HeroClass <|.. Chaos
HeroClass <|.. Warrior
HeroClass <|.. Mage

Hero --> HeroClass : has

%% =========================
%% ABILITY
%% =========================

class Ability {
    -String name
    -int manaCost
    +execute(caster: Hero, targets: List~Hero~) void
}

class Protect
class Heal
class Fireball
class ChainLightning
class BerserkerAttack
class Replenish

Ability <|-- Protect
Ability <|-- Heal
Ability <|-- Fireball
Ability <|-- ChainLightning
Ability <|-- BerserkerAttack
Ability <|-- Replenish

HeroClass --> Ability : provides >

%% =========================
%% BATTLE
%% =========================

class Battle {
    -List~Hero~ teamA
    -List~Hero~ teamB
    -Queue~Hero~ turnQueue

    +startBattle() void
    +determineTurnOrder() void
    +processTurn(activeHero: Hero) void
    +checkWinCondition() boolean
    +getWinner() List~Hero~
}

Battle --> Hero : manages >

%% =========================
%% STATUS EFFECT
%% =========================

class StatusEffect {
    -StatusType type
    -int duration

    +apply(hero: Hero) void
    +tick() void
    +isExpired() boolean
}

class StatusType {
    <<enumeration>>
    STUN
    SHIELD
}

Hero --> StatusEffect : has >
StatusEffect --> StatusType