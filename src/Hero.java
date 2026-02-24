package src;

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

    public Hero(String name, HeroClass heroClass) {
        this.name = name;
        this.heroClass = heroClass;
        this.level = 1;
        this.attack = 5;
        this.defense = 5;
        this.healthPoints = 100;
        this.manaPoints = 50;
        this.experience = 0;
        this.isStunned = false;
        this.isAlive = true;
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

    public void heal(int amount) {
        healthPoints += amount;
        // Optionally cap at max HP
    }

    public void useMana(int amount) {
        manaPoints -= amount;
        if (manaPoints < 0) manaPoints = 0;
    }

    public void gainExperience(int exp) {
        experience += exp;
        // Level up logic to be expanded
    }

    public void setStunned(boolean stunned) {
        isStunned = stunned;
    }

    public void revive() {
        isAlive = true;
        healthPoints = 100; // Or max HP
        manaPoints = 50;    // Or max mana
    }
}
