package model;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import model.heroclass.OrderStrategy;
import model.heroclass.ChaosStrategy;
import model.heroclass.WarriorStrategy;
import model.heroclass.MageStrategy;

/**
 * Represents a hero in the Legends of Sword and Wand RPG.
 * With the use of AI
 */
public class Hero {
    private String name;
    private HeroClass heroClass;
    private int level;
    private int experience;
    private boolean isStunned;
    private boolean isAlive;

    private Map<HeroClass, Integer> classLevels;
    private HeroClass specializationClass; // First class to reach level 5
    private HeroClass hybridClass;         // Set when two classes reach level 5
    private HeroClassStrategy classStrategy; // Active Strategy pattern implementation
    private int baseAttack;
    private int baseDefense;
    private int maxHealth;
    private int currentHealth;
    private int maxMana;
    private int currentMana;
    private int shieldAmount; // Active shield HP absorbed before taking real damage

    private List<StatusEffect> statusEffects;
    private final List<HeroObserver> observers = new ArrayList<>();

    public Hero(String name, HeroClass heroClass) {
        this.name = name;
        this.heroClass = heroClass;
        this.level = 1;
        this.baseAttack = 5;
        this.baseDefense = 5;
        this.maxHealth = 100;
        this.currentHealth = 100;
        this.maxMana = 50;
        this.currentMana = 50;
        this.classLevels = new HashMap<>();
        this.classLevels.put(heroClass, 1);
        this.classStrategy = createStrategy(heroClass);
        // Apply per-level bonuses for the initial class level (level 1)
        this.classStrategy.applyLevelBonus(this);
        // Ensure current HP/MP reflect any max changes from the class bonus
        this.currentHealth = getCurrentMaxHealth();
        this.currentMana = getCurrentMaxMana();
        this.specializationClass = null;
        this.hybridClass = null;
        this.experience = 0;
        this.isStunned = false;
        this.isAlive = true;
        this.statusEffects = new ArrayList<>();
    }

    // Getters
    public String getName()          {
         return name;
    }
    public void setName(String newName) {
        if (newName != null && !newName.isBlank()) this.name = newName;
    }
    public HeroClass getHeroClass()  {
         return heroClass;
    }
    public int getLevel()            {
         return level;
    }
    public int getExperience()       {
         return experience;
    }
    public boolean isStunned()       {
         return isStunned;
    }
    public boolean isAlive()         {
         return isAlive;
    }
    public int getCurrentHealth()    {
         return currentHealth;
    }
    public int getCurrentMana()      {
         return currentMana;
    }
    public int getShieldAmount()     {
         return shieldAmount;
    }

    // Setters used by StatusEffect / abilities
    public void setStunned(boolean stunned)      {
        this.isStunned = stunned;
    }
    public void setShieldAmount(int shield)      {
        this.shieldAmount = shield;
    }
    public void addShield(int amount)            {
        this.shieldAmount += amount;
    }

    // Stat mutators used by HeroClassStrategy implementations
    public void addBaseAttack(int amount)        {
        this.baseAttack += amount;
    }
    public void addBaseDefense(int amount)       {
        this.baseDefense += amount;
    }
    public void addMaxHealth(int amount)         {
        this.maxHealth += amount;
    }
    public void addMaxMana(int amount)           {
        this.maxMana += amount;
    }

    /**
     * Level up a specific class. Handles specialization and hybrid logic.
     */
    public void levelUp(HeroClass classType) {
        int currentClassLevel = classLevels.getOrDefault(classType, 0);
        classLevels.put(classType, currentClassLevel + 1);
        level++;
        // Base stat growth
        baseAttack += 1;
        baseDefense += 1;
        maxHealth += 5;
        maxMana += 2;
        // Apply class-specific bonuses
        applyClassBonuses(classType);
        // Specialization logic: set the first class to reach level 5 as specialization.
        // Hybridization (choosing a second class) is handled via the UI prompt so
        // we don't automatically assign `hybridClass` here.
        if (classLevels.get(classType) == 5 && specializationClass == null) {
            specializationClass = classType;
        }
        notifyLevelUp(level);
    }

    /**
     * Factory: maps a HeroClass enum value to its Strategy implementation.
     */
    private HeroClassStrategy createStrategy(HeroClass classType) {
        switch (classType) {
            case ORDER:   return new OrderStrategy();
            case CHAOS:   return new ChaosStrategy();
            case WARRIOR: return new WarriorStrategy();
            case MAGE:    return new MageStrategy();
            default:      return new WarriorStrategy(); // fallback for HYBRID
        }
    }

    /**
     * Apply class-specific stat bonuses per level by delegating to the Strategy.
     */
    private void applyClassBonuses(HeroClass classType) {
        // Update active strategy when levelling a different class
        classStrategy = createStrategy(classType);
        classStrategy.applyLevelBonus(this);
    }

    /** Returns the abilities available to this hero's current class strategy. */
    public List<Ability> getClassAbilities() {
        return classStrategy.getAbilities(this);
    }


    public int getClassLevel(HeroClass c) {
        return classLevels.getOrDefault(c, 0);
    }

    /**
     * Re-evaluate specialization and hybrid fields based on current `classLevels`.
     * Useful when a hero was loaded from persistence or mutated externally
     * and you need to ensure `specializationClass` / `hybridClass` reflect
     * classes that have reached level 5.
     */
    public void recomputeSpecializationFromLevels() {
        // find all classes at or above level 5
        java.util.List<HeroClass> high = new java.util.ArrayList<>();
        for (java.util.Map.Entry<HeroClass, Integer> e : classLevels.entrySet()) {
            if (e.getValue() >= 5) high.add(e.getKey());
        }

        if (high.isEmpty()) return;

        // Ensure specialization is set to the first qualifying class if not already set
        if (specializationClass == null && !high.isEmpty()) {
            specializationClass = high.get(0);
        }
    }

    /**
     * Calculate current attack (base + class/hybrid bonuses).
     */
    public int getCurrentAttack() {
        int attack = baseAttack;
        // Double growth if specialized and not hybrid
        if (specializationClass != null && hybridClass == null) {
            if (specializationClass == HeroClass.WARRIOR) attack += 1 * classLevels.getOrDefault(HeroClass.WARRIOR, 0);
            if (specializationClass == HeroClass.CHAOS) attack += 3 * classLevels.getOrDefault(HeroClass.CHAOS, 0);
            if (specializationClass == HeroClass.MAGE) attack += 1 * classLevels.getOrDefault(HeroClass.MAGE, 0);
        }
        // Hybrid logic: combine growths, no double
        if (hybridClass != null && specializationClass != null) {
            // Example: combine both class growths
            attack += getHybridAttackBonus();
        }
        return attack;
    }

    private int getHybridAttackBonus() {
        int bonus = 0;
        if (specializationClass != null && hybridClass != null) {
            bonus += getClassAttackBonus(specializationClass);
            bonus += getClassAttackBonus(hybridClass);
        }
        return bonus;
    }

    private int getClassAttackBonus(HeroClass classType) {
        switch (classType) {
            case WARRIOR: return 2 * classLevels.getOrDefault(HeroClass.WARRIOR, 0);
            case CHAOS: return 3 * classLevels.getOrDefault(HeroClass.CHAOS, 0);
            case MAGE: return 1 * classLevels.getOrDefault(HeroClass.MAGE, 0);
            default: return 0;
        }
    }

    /**
     * Calculate current defense (base + class/hybrid bonuses).
     */
    public int getCurrentDefense() {
        int defense = baseDefense;
        if (specializationClass != null && hybridClass == null) {
            if (specializationClass == HeroClass.ORDER) defense += 2 * classLevels.getOrDefault(HeroClass.ORDER, 0);
            if (specializationClass == HeroClass.WARRIOR) defense += 3 * classLevels.getOrDefault(HeroClass.WARRIOR, 0);
        }
        if (hybridClass != null && specializationClass != null) {
            defense += getHybridDefenseBonus();
        }
        return defense;
    }

    private int getHybridDefenseBonus() {
        int bonus = 0;
        if (specializationClass != null && hybridClass != null) {
            bonus += getClassDefenseBonus(specializationClass);
            bonus += getClassDefenseBonus(hybridClass);
        }
        return bonus;
    }

    private int getClassDefenseBonus(HeroClass classType) {
        switch (classType) {
            case ORDER: return 2 * classLevels.getOrDefault(HeroClass.ORDER, 0);
            case WARRIOR: return 3 * classLevels.getOrDefault(HeroClass.WARRIOR, 0);
            default: return 0;
        }
    }

    /**
     * Calculate current max health (base + class/hybrid bonuses).
     */
    public int getCurrentMaxHealth() {
        int hp = maxHealth;
        if (specializationClass != null && hybridClass == null) {
            if (specializationClass == HeroClass.CHAOS) hp += 5 * classLevels.getOrDefault(HeroClass.CHAOS, 0);
        }
        if (hybridClass != null && specializationClass != null) {
            hp += getHybridHealthBonus();
        }
        return hp;
    }

    private int getHybridHealthBonus() {
        int bonus = 0;
        if (specializationClass != null && hybridClass != null) {
            bonus += getClassHealthBonus(specializationClass);
            bonus += getClassHealthBonus(hybridClass);
        }
        return bonus;
    }

    private int getClassHealthBonus(HeroClass classType) {
        switch (classType) {
            case CHAOS: return 5 * classLevels.getOrDefault(HeroClass.CHAOS, 0);
            default: return 0;
        }
    }

    /**
     * Calculate current max mana (base + class/hybrid bonuses).
     */
    public int getCurrentMaxMana() {
        int mana = maxMana;
        if (specializationClass != null && hybridClass == null) {
            if (specializationClass == HeroClass.ORDER) mana += 5 * classLevels.getOrDefault(HeroClass.ORDER, 0);
            if (specializationClass == HeroClass.MAGE) mana += 5 * classLevels.getOrDefault(HeroClass.MAGE, 0);
        }
        if (hybridClass != null && specializationClass != null) {
            mana += getHybridManaBonus();
        }
        return mana;
    }

    private int getHybridManaBonus() {
        int bonus = 0;
        if (specializationClass != null && hybridClass != null) {
            bonus += getClassManaBonus(specializationClass);
            bonus += getClassManaBonus(hybridClass);
        }
        return bonus;
    }

    private int getClassManaBonus(HeroClass classType) {
        switch (classType) {
            case ORDER: return 5 * classLevels.getOrDefault(HeroClass.ORDER, 0);
            case MAGE: return 5 * classLevels.getOrDefault(HeroClass.MAGE, 0);
            default: return 0;
        }
    }

    /**
     * Apply incoming damage. Shield absorbs first, then real HP is reduced.
     */
    public void takeDamage(int damage) {
        if (shieldAmount > 0) {
            int absorbed = Math.min(shieldAmount, damage);
            shieldAmount -= absorbed;
            damage -= absorbed;
        }
        currentHealth -= damage;
        if (currentHealth <= 0) {
            currentHealth = 0;
            isAlive = false;
            notifyDied();
        }
    }

    public void useMana(int amount) {
        currentMana -= amount;
        if (currentMana < 0) currentMana = 0;
    }

    // Status effect handling
    public void addStatusEffect(StatusEffect effect) {
        statusEffects.add(effect);
        effect.apply(this);
    }

    public void processStatusEffects() {
        List<StatusEffect> expired = new ArrayList<>();
        for (StatusEffect effect : statusEffects) {
            effect.tick();
            if (effect.isExpired()) {
                effect.expire(this);
                expired.add(effect);
            }
        }
        statusEffects.removeAll(expired);
    }

    public boolean hasStatus(StatusType type) {
        for (StatusEffect effect : statusEffects) {
            if (effect.getType() == type && !effect.isExpired()) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Observer registration
    // -------------------------------------------------------------------------

    public void addObserver(HeroObserver observer) {
        if (observer != null && !observers.contains(observer)) observers.add(observer);
    }

    public void removeObserver(HeroObserver observer) {
        observers.remove(observer);
    }

    private void notifyDied()                   { observers.forEach(o -> o.onHeroDied(this)); }
    private void notifyRevived()                { observers.forEach(o -> o.onHeroRevived(this)); }
    private void notifyLevelUp(int newLevel)    { observers.forEach(o -> o.onLevelUp(this, newLevel)); }
    private void notifyHealed(int amountHealed) { observers.forEach(o -> o.onHeroHealed(this, amountHealed)); }

    // -------------------------------------------------------------------------
    // Combat actions
    // -------------------------------------------------------------------------

    /**
     * Basic attack against a single target.
     * Damage formula: max(0, attacker.getCurrentAttack() - defender.getCurrentDefense())
     * Returns the actual damage dealt (after shield absorption) for logging purposes.
     */
    public int attack(Hero target) {
        // Ensure an attack always does at least 1 damage to make combat meaningful.
        int damage = Math.max(1, this.getCurrentAttack() - target.getCurrentDefense());
        target.takeDamage(damage);
        return damage;
    }

    /**
     * Defend action — forfeits this hero's turn.
     * Restores a portion of HP and mana. Previously this was a fixed
     * +10 HP / +5 mana which made low-HP enemies effectively immortal when
     * they could defend repeatedly. This scales restore with the hero's
     * max values: roughly 10% of max HP and 5% of max mana (minimum 1).
     */
    public void defend() {
        int healAmount = Math.max(1, getCurrentMaxHealth() / 10); // ~10% max HP
        int manaAmount = Math.max(1, getCurrentMaxMana() / 20);   // ~5% max MP
        heal(healAmount);
        restoreMana(manaAmount);
    }

    /**
     * Wait action — signals that this hero wants to defer their action to the
     * end of the turn (FIFO queue).  The actual queuing is managed by
     * BattleServiceImpl; this method exists so Hero satisfies the UML contract
     * and can be called uniformly in takeTurn().
     */
    public void waitTurn() {
        // Intentionally empty — deferral logic is handled by the battle orchestrator.
        // Keeping this here preserves the UML method contract on Hero.
    }

    // -------------------------------------------------------------------------
    // Ability and mana management
    // -------------------------------------------------------------------------

    public boolean canCast(Ability ability) {
        return currentMana >= ability.getManaCost();
    }

    public void castAbility(Ability ability, List<Hero> targets) {
        if (!canCast(ability)) throw new IllegalStateException("Not enough mana");
        currentMana -= ability.getManaCost();
        ability.execute(this, targets);
    }

    // Experience and leveling
    public void gainExperience(int exp) {
        experience += exp;
        while (experience >= getExpToLevelUp()) {
            experience -= getExpToLevelUp();
            levelUp(heroClass); // Default: level up current class
        }
    }

    /**
     * Reduces current level-progress experience by a percentage.
     * This does not reduce hero levels and clamps at 0.
     */
    public void applyExperiencePenalty(double penaltyFraction) {
        if (penaltyFraction <= 0) return;
        int penalty = (int) Math.floor(experience * penaltyFraction);
        experience = Math.max(0, experience - penalty);
    }

    private int getExpToLevelUp() {
        // Reduced threshold for faster testing: base 100 + 20*L + 10*L^2
        // (Previously 500 + 75*L + 20*L^2 — lowered to speed up leveling during development)
        return 10 + 20 * level + 10 * level * level;
    }

    // Utility methods
    public void revive() {
        isAlive = true;
        currentHealth = getCurrentMaxHealth();
        currentMana = getCurrentMaxMana();
        notifyRevived();
    }

    public void restoreMana(int amount) {
        currentMana = Math.min(currentMana + amount, getCurrentMaxMana());
    }

    public void heal(int amount) {
        int before = currentHealth;
        currentHealth = Math.min(currentHealth + amount, getCurrentMaxHealth());
        int healed = currentHealth - before;
        if (healed > 0) notifyHealed(healed);
    }

    /**
     * Create a deep copy of this Hero suitable for snapshotting into saved parties.
     * Observers are NOT copied.
     */
    public Hero copy() {
        Hero h = new Hero(this.name, this.heroClass);
        h.level = this.level;
        h.experience = this.experience;
        h.isStunned = this.isStunned;
        h.isAlive = this.isAlive;
        h.baseAttack = this.baseAttack;
        h.baseDefense = this.baseDefense;
        h.maxHealth = this.maxHealth;
        h.currentHealth = this.currentHealth;
        h.maxMana = this.maxMana;
        h.currentMana = this.currentMana;
        h.shieldAmount = this.shieldAmount;
        // Copy classLevels map
        h.classLevels = new java.util.HashMap<>(this.classLevels);
        h.specializationClass = this.specializationClass;
        h.hybridClass = this.hybridClass;
        // Recreate strategy for the copied hero
        h.classStrategy = h.createStrategy(h.heroClass);
        // Copy status effects shallowly (effects are immutable-like or will be reset on revive in practice)
        h.statusEffects = new java.util.ArrayList<>(this.statusEffects);
        // Observers intentionally not copied
        return h;
    }

    // Getters for specialization/hybrid for testing
    public HeroClass getSpecializationClass() {
        return specializationClass;
    }
    public HeroClass getHybridClass() {
        return hybridClass;
    }

    /**
     * Human-friendly name for the hero's specialization or hybrid.
     * Examples: WARRIOR -> "Knight", MAGE -> "Wizard", ORDER+WARRIOR -> "Paladin".
     * Returns null if no specialization/hybrid is present.
     */
    public String getSpecializationDisplayName() {
        if (specializationClass == null) return null;
        // Hybrid specific names
        if (hybridClass != null) {
            // Paladin: Order + Warrior
            if ((specializationClass == HeroClass.WARRIOR && hybridClass == HeroClass.ORDER)
                    || (specializationClass == HeroClass.ORDER && hybridClass == HeroClass.WARRIOR)) {
                return "Paladin";
            }
            // Fallback: combine names
            return specializationClass.name() + "+" + hybridClass.name();
        }

        // Single-class specialization names
        switch (specializationClass) {
            case WARRIOR: return "Knight";
            case MAGE: return "Wizard";
            case ORDER: return "Priest";
            case CHAOS: return "Dread"; // flavor name for Chaos specialization
            default: return specializationClass.name();
        }
    }

    /**
     * Set the hero's hybrid class. Use when player chooses to hybridize.
     */
    public void setHybridClass(HeroClass hybrid) {
        this.hybridClass = hybrid;
    }
}
