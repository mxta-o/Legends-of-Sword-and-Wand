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
        // Specialization logic
        if (classLevels.get(classType) == 5 && specializationClass == null) {
            specializationClass = classType;
        } else if (classLevels.get(classType) == 5 && specializationClass != null && hybridClass == null && specializationClass != classType) {
            hybridClass = classType; // Now hybridized
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
        return classStrategy.getAbilities();
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
        int damage = Math.max(0, this.getCurrentAttack() - target.getCurrentDefense());
        target.takeDamage(damage);
        return damage;
    }

    /**
     * Defend action — forfeits this hero's turn.
     * Restores +10 HP and +5 mana as per the game spec.
     */
    public void defend() {
        heal(10);
        restoreMana(5);
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
        // Example: Exp(L) = Exp(L-1)+500+75*L+20*L^2
        return 500 + 75 * level + 20 * level * level;
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

    // Getters for specialization/hybrid for testing
    public HeroClass getSpecializationClass() {
        return specializationClass;
    }
    public HeroClass getHybridClass() {
        return hybridClass;
    }
}
