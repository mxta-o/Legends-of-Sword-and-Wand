package src;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a hero in the Legends of Sword and Wand RPG.
 */
public class Hero {
    private String name;
    private HeroClass heroClass;
    private int level;
    private int attack;
    private int defense;
    private int healthPoints;
    private int manaPoints;
    private int experience;
    private boolean isStunned;
    private boolean isAlive;

    private Map<HeroClass, Integer> classLevels;
    private HeroClass specializationClass; // First class to reach level 5
    private HeroClass hybridClass; // Set when two classes reach level 5
    private int baseAttack;
    private int baseDefense;
    private int maxHealth;
    private int currentHealth;
    private int maxMana;
    private int currentMana;

    private List<StatusEffect> statusEffects;

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
        this.specializationClass = null;
        this.hybridClass = null;
        this.experience = 0;
        this.isStunned = false;
        this.isAlive = true;
        this.statusEffects = new ArrayList<>();
    }

    // Getters and setters
    public String getName() {
        return name;
    }
    public HeroClass getHeroClass() {
        return heroClass;
    }
    public int getLevel() {
        return level;
    }
    public int getAttack() {
        return attack;
    }
    public int getDefense() {
        return defense;
    }
    public int getHealthPoints() {
        return healthPoints;
    }
    public int getManaPoints() {
        return manaPoints;
    }
    public int getExperience() {
        return experience;
    }
    public boolean isStunned() {
        return isStunned;
    }
    public boolean isAlive() {
        return isAlive;
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
    }

    /**
     * Apply class-specific stat bonuses per level.
     */
    private void applyClassBonuses(HeroClass classType) {
        switch (classType) {
            case ORDER:
                maxMana += 5;
                baseDefense += 2;
                break;
            case CHAOS:
                baseAttack += 3;
                maxHealth += 5;
                break;
            case WARRIOR:
                baseAttack += 2;
                baseDefense += 3;
                break;
            case MAGE:
                maxMana += 5;
                baseAttack += 1;
                break;
            default:
                break;
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

    // Level up method (basic, without class bonuses)
    public void levelUp() {
        level++;
        attack++;
        defense++;
        healthPoints += 5;
        manaPoints += 2;
        // Class bonuses to be added later
    }

    // Methods for taking damage, healing, using mana, etc.
    public void takeDamage(int damage) {
        healthPoints -= damage;
        if (healthPoints <= 0) {
            healthPoints = 0;
            isAlive = false;
        }
    }

    public void useMana(int amount) {
        manaPoints -= amount;
        if (manaPoints < 0) manaPoints = 0;
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
            if (effect.isExpired()) expired.add(effect);
        }
        statusEffects.removeAll(expired);
    }

    public boolean hasStatus(StatusType type) {
        for (StatusEffect effect : statusEffects) {
            if (effect.getType() == type && !effect.isExpired()) return true;
        }
        return false;
    }

    // Ability and mana management
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

    private int getExpToLevelUp() {
        // Example: Exp(L) = Exp(L-1)+500+75*L+20*L^2
        return 500 + 75 * level + 20 * level * level;
    }

    // Utility methods
    public void revive() {
        isAlive = true;
        currentHealth = getCurrentMaxHealth();
        currentMana = getCurrentMaxMana();
    }

    public void restoreMana(int amount) {
        currentMana = Math.min(currentMana + amount, getCurrentMaxMana());
    }

    public void heal(int amount) {
        currentHealth = Math.min(currentHealth + amount, getCurrentMaxHealth());
    }

    // Getters for specialization/hybrid for testing
    public HeroClass getSpecializationClass() {
        return specializationClass;
    }
    public HeroClass getHybridClass() {
        return hybridClass;
    }
}
