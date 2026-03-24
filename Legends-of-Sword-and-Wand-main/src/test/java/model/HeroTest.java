package model;

import model.ability.BerserkerAttack;
import model.ability.ChainLightning;
import model.ability.Fireball;
import model.ability.Heal;
import model.ability.Protect;
import model.ability.Replenish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeroTest {

    // =========================================================================
    // 1. Hero initial state
    // =========================================================================

    @Test
    public void testHeroStartingStats() {
        // Spec: every hero starts with 5 atk, 5 def, 100 HP, 50 mana, level 1
        Hero hero = new Hero("Alice", HeroClass.WARRIOR);
        assertEquals(1,   hero.getLevel());
        assertEquals(100, hero.getCurrentHealth());
        assertEquals(50,  hero.getCurrentMana());
        assertTrue(hero.isAlive());
        assertFalse(hero.isStunned());
    }

    // =========================================================================
    // 2. Level-up: base stat growth per level
    // =========================================================================

    @Test
    public void testBaseLevelUpStatGrowth() {
        // Spec: each level gives +1 atk, +1 def, +5 HP, +2 mana (before class bonuses)
        // Warrior also adds +2 atk, +3 def per level
        Hero hero = new Hero("Bob", HeroClass.WARRIOR);
        int hpBefore   = hero.getCurrentMaxHealth();
        int manaBefore = hero.getCurrentMaxMana();
        hero.levelUp(HeroClass.WARRIOR);
        assertEquals(hpBefore   + 5, hero.getCurrentMaxHealth());
        assertEquals(manaBefore + 2, hero.getCurrentMaxMana()); // +2 base (Warrior adds no mana)
        assertEquals(2, hero.getLevel());
    }

    // =========================================================================
    // 3. Class-specific stat bonuses
    // =========================================================================

    @Test
    public void testOrderClassBonusesPerLevel() {
        // Order: +5 mana, +2 defense per level
        Hero hero = new Hero("Claire", HeroClass.ORDER);
        int manaBefore    = hero.getCurrentMaxMana();
        int defenseBefore = hero.getCurrentDefense();
        hero.levelUp(HeroClass.ORDER);
        assertEquals(manaBefore    + 2 + 5, hero.getCurrentMaxMana());   // +2 base +5 Order
        assertEquals(defenseBefore + 1 + 2, hero.getCurrentDefense());   // +1 base +2 Order
    }

    @Test
    public void testChaosClassBonusesPerLevel() {
        // Chaos: +3 attack, +5 health per level
        Hero hero = new Hero("Dave", HeroClass.CHAOS);
        int hpBefore  = hero.getCurrentMaxHealth();
        int atkBefore = hero.getCurrentAttack();
        hero.levelUp(HeroClass.CHAOS);
        assertEquals(hpBefore  + 5 + 5, hero.getCurrentMaxHealth()); // +5 base +5 Chaos
        assertEquals(atkBefore + 1 + 3, hero.getCurrentAttack());    // +1 base +3 Chaos
    }

    // =========================================================================
    // 4. Specialization: first class to reach level 5
    // =========================================================================

    @Test
    public void testSpecializationSetAtClassLevel5() {
        Hero hero = new Hero("Eve", HeroClass.WARRIOR);
        // Hero starts at class level 1, so 4 more level-ups reach class level 5
        for (int i = 0; i < 4; i++) hero.levelUp(HeroClass.WARRIOR);
        assertEquals(HeroClass.WARRIOR, hero.getSpecializationClass());
        assertNull(hero.getHybridClass());
    }

    @Test
    public void testNoSpecializationBeforeLevel5() {
        Hero hero = new Hero("Frank", HeroClass.ORDER);
        for (int i = 0; i < 3; i++) hero.levelUp(HeroClass.ORDER);
        assertNull(hero.getSpecializationClass()); // only 4 levels in ORDER, not 5
    }

    // =========================================================================
    // 5. Hybridization: second class to reach level 5
    // =========================================================================

    @Test
    public void testHybridizationSetWhenSecondClassReachesLevel5() {
        Hero hero = new Hero("Grace", HeroClass.WARRIOR);
        for (int i = 0; i < 4; i++) hero.levelUp(HeroClass.WARRIOR);  // WARRIOR spec
        assertEquals(HeroClass.WARRIOR, hero.getSpecializationClass());
        for (int i = 0; i < 5; i++) hero.levelUp(HeroClass.MAGE);     // MAGE hybrid
        assertEquals(HeroClass.MAGE, hero.getHybridClass());
    }

    @Test
    public void testSameClassDoesNotTriggerHybrid() {
        // Levelling the SAME class beyond 5 should not set hybridClass
        Hero hero = new Hero("Hank", HeroClass.CHAOS);
        for (int i = 0; i < 4; i++) hero.levelUp(HeroClass.CHAOS); // spec at level 5
        hero.levelUp(HeroClass.CHAOS);                               // level 6 in CHAOS
        assertNull(hero.getHybridClass());
    }

    // =========================================================================
    // 6. Basic attack damage formula
    // =========================================================================

    @Test
    public void testBasicAttackDamageFormula() {
        // Damage = max(0, attackerATK - defenderDEF)
        Hero attacker = new Hero("Ivan",  HeroClass.CHAOS);
        Hero defender = new Hero("Julia", HeroClass.ORDER);
        // Level them up a bit so attacker has more ATK than defender DEF
        attacker.levelUp(HeroClass.CHAOS);   // +1 base +3 Chaos atk
        int expectedDamage = Math.max(0, attacker.getCurrentAttack() - defender.getCurrentDefense());
        int hpBefore = defender.getCurrentHealth();
        attacker.attack(defender);
        assertEquals(hpBefore - expectedDamage, defender.getCurrentHealth());
    }

    @Test
    public void testAttackCannotDealNegativeDamage() {
        // If defender DEF >= attacker ATK, damage should be 0
        Hero weakAttacker = new Hero("Karl",  HeroClass.ORDER);  // low ATK
        Hero tankDefender = new Hero("Laura", HeroClass.WARRIOR);
        // Give tank lots of defense via level-ups
        for (int i = 0; i < 5; i++) tankDefender.levelUp(HeroClass.WARRIOR); // +5*(1+3) def
        int hpBefore = tankDefender.getCurrentHealth();
        weakAttacker.attack(tankDefender);
        assertTrue(tankDefender.getCurrentHealth() >= hpBefore - 1); // 0 or minimal damage
    }

    // =========================================================================
    // 7. Defend action
    // =========================================================================

    @Test
    public void testDefendRestoresHpAndMana() {
        // Spec: defend gives +10 HP, +5 mana
        Hero hero = new Hero("Mike", HeroClass.WARRIOR);
        hero.takeDamage(30);
        hero.useMana(20);
        int hpBefore   = hero.getCurrentHealth();
        int manaBefore = hero.getCurrentMana();
        hero.defend();
        assertEquals(hpBefore   + 10, hero.getCurrentHealth());
        assertEquals(manaBefore + 5,  hero.getCurrentMana());
    }

    @Test
    public void testDefendDoesNotExceedMaxStats() {
        // Defend on a full-HP, full-mana hero should clamp at max
        Hero hero = new Hero("Nina", HeroClass.WARRIOR);
        hero.defend();
        assertEquals(hero.getCurrentMaxHealth(), hero.getCurrentHealth());
        assertEquals(hero.getCurrentMaxMana(),   hero.getCurrentMana());
    }

    // =========================================================================
    // 8. takeDamage and shield absorption
    // =========================================================================

    @Test
    public void testShieldAbsorbsDamageFirst() {
        Hero hero = new Hero("Owen", HeroClass.ORDER);
        hero.addShield(20);
        hero.takeDamage(15); // fully absorbed by shield
        assertEquals(100, hero.getCurrentHealth()); // HP untouched
        assertEquals(5,   hero.getShieldAmount());  // 20 - 15 = 5 shield remaining
    }

    @Test
    public void testShieldPartiallyAbsorbsOverflowDamage() {
        Hero hero = new Hero("Pam", HeroClass.ORDER);
        hero.addShield(10);
        hero.takeDamage(30); // 10 absorbed, 20 hits HP
        assertEquals(80, hero.getCurrentHealth());
        assertEquals(0,  hero.getShieldAmount());
    }

    @Test
    public void testHeroDiesWhenHpReachesZero() {
        Hero hero = new Hero("Quinn", HeroClass.WARRIOR);
        hero.takeDamage(100);
        assertEquals(0, hero.getCurrentHealth());
        assertFalse(hero.isAlive());
    }

    // =========================================================================
    // 9. Stun status effect
    // =========================================================================

    @Test
    public void testStunPreventsActionForOneTurn() {
        Hero hero = new Hero("Rob", HeroClass.WARRIOR);
        hero.addStatusEffect(new StatusEffect(StatusType.STUN, 1));
        assertTrue(hero.isStunned());
        // After one tick the stun expires
        hero.processStatusEffects();
        assertFalse(hero.isStunned());
    }

    // =========================================================================
    // 10. Protect ability — shield on all allies
    // =========================================================================

    @Test
    public void testProtectShieldsAllAllies() {
        // Protect: 10% of each ally's max HP as shield
        Hero caster = new Hero("Sara", HeroClass.ORDER);
        Hero ally1  = new Hero("Tom",  HeroClass.WARRIOR);
        Hero ally2  = new Hero("Uma",  HeroClass.CHAOS);
        List<Hero> team = Arrays.asList(caster, ally1, ally2);

        Protect protect = new Protect();
        int manaBefore = caster.getCurrentMana();
        caster.castAbility(protect, team);

        assertEquals(manaBefore - 25, caster.getCurrentMana()); // 25 mana cost
        int expectedShield = (int)(ally1.getCurrentMaxHealth() * 0.10);
        assertEquals(expectedShield, ally1.getShieldAmount());
        assertEquals(expectedShield, ally2.getShieldAmount());
    }

    // =========================================================================
    // 11. Heal ability — heals lowest HP ally
    // =========================================================================

    @Test
    public void testHealTargetsLowestHpAlly() {
        Hero caster  = new Hero("Vera",  HeroClass.ORDER);
        Hero healthy = new Hero("Will",  HeroClass.WARRIOR);
        Hero injured = new Hero("Xena",  HeroClass.CHAOS);
        injured.takeDamage(60); // injured has 40 HP, healthy has 100
        List<Hero> team = Arrays.asList(caster, healthy, injured);

        int injuredHpBefore = injured.getCurrentHealth();
        int expectedHeal = (int)(injured.getCurrentMaxHealth() * 0.25);
        caster.castAbility(new Heal(), team);

        assertEquals(injuredHpBefore + expectedHeal, injured.getCurrentHealth());
        assertEquals(100, healthy.getCurrentHealth()); // healthy hero untouched
    }

    // =========================================================================
    // 12. Fireball ability — hits up to 3 enemies
    // =========================================================================

    @Test
    public void testFireballHitsUpToThreeTargets() {
        Hero caster  = new Hero("Yara",  HeroClass.CHAOS);
        Hero enemy1  = new Hero("Zack",  HeroClass.WARRIOR);
        Hero enemy2  = new Hero("Alpha", HeroClass.WARRIOR);
        Hero enemy3  = new Hero("Beta",  HeroClass.WARRIOR);
        Hero enemy4  = new Hero("Gamma", HeroClass.WARRIOR); // 4th should NOT be hit
        List<Hero> enemies = Arrays.asList(enemy1, enemy2, enemy3, enemy4);

        int hp4Before = enemy4.getCurrentHealth();
        caster.castAbility(new Fireball(), enemies);

        // All first 3 should have taken some damage (attacker ATK=5 vs defender DEF=5 => 0 dmg at level 1)
        // Give caster a level-up to ensure positive damage
        Hero strongCaster = new Hero("Delta", HeroClass.CHAOS);
        for (int i = 0; i < 3; i++) strongCaster.levelUp(HeroClass.CHAOS); // +9 atk over base
        Hero t1 = new Hero("E1", HeroClass.ORDER);
        Hero t2 = new Hero("E2", HeroClass.ORDER);
        Hero t3 = new Hero("E3", HeroClass.ORDER);
        Hero t4 = new Hero("E4", HeroClass.ORDER);
        int t4HpBefore = t4.getCurrentHealth();
        strongCaster.castAbility(new Fireball(), Arrays.asList(t1, t2, t3, t4));
        assertTrue(t1.getCurrentHealth() < 100);
        assertTrue(t2.getCurrentHealth() < 100);
        assertTrue(t3.getCurrentHealth() < 100);
        assertEquals(t4HpBefore, t4.getCurrentHealth()); // 4th target NOT hit
    }

    // =========================================================================
    // 13. BerserkerAttack — primary + 2 splash at 25%
    // =========================================================================

    @Test
    public void testBerserkerAttackHitsPrimaryAndTwoSplash() {
        Hero attacker = new Hero("Epsilon", HeroClass.WARRIOR);
        for (int i = 0; i < 4; i++) attacker.levelUp(HeroClass.WARRIOR); // boost ATK
        Hero primary = new Hero("F1", HeroClass.ORDER);
        Hero splash1 = new Hero("F2", HeroClass.ORDER);
        Hero splash2 = new Hero("F3", HeroClass.ORDER);

        int primaryDamage = Math.max(0, attacker.getCurrentAttack() - primary.getCurrentDefense());
        int splashDamage  = (int)(primaryDamage * 0.25);

        // Call execute() directly — BerserkerAttack costs 60 mana, base hero only has 50
        new BerserkerAttack().execute(attacker, Arrays.asList(primary, splash1, splash2));

        assertEquals(100 - primaryDamage, primary.getCurrentHealth());
        assertEquals(100 - splashDamage,  splash1.getCurrentHealth());
        assertEquals(100 - splashDamage,  splash2.getCurrentHealth());
    }

    // =========================================================================
    // 14. Replenish ability — mana restore
    // =========================================================================

    @Test
    public void testReplenishRestoresManaToAlliesAndMoreToSelf() {
        Hero caster = new Hero("Zeta", HeroClass.MAGE);
        Hero ally   = new Hero("Eta",  HeroClass.WARRIOR);
        caster.useMana(30); // drain partially — keep some so restore doesn't hit cap
        ally.useMana(30);

        int casterManaBefore = caster.getCurrentMana(); // 20
        int allyManaBefore   = ally.getCurrentMana();   // 20

        // Call execute() directly to test ability logic in isolation (cost check bypassed)
        new Replenish().execute(caster, Arrays.asList(caster, ally));

        // Caster gets +60 (capped at max), ally gets +30 (capped at max)
        int expectedCaster = Math.min(casterManaBefore + 60, caster.getCurrentMaxMana());
        int expectedAlly   = Math.min(allyManaBefore   + 30, ally.getCurrentMaxMana());
        assertEquals(expectedCaster, caster.getCurrentMana());
        assertEquals(expectedAlly,   ally.getCurrentMana());
    }

    // =========================================================================
    // 15. Revive restores full HP and mana
    // =========================================================================

    @Test
    public void testReviveRestoresHeroToFull() {
        Hero hero = new Hero("Theta", HeroClass.WARRIOR);
        hero.takeDamage(100); // kill
        assertFalse(hero.isAlive());
        hero.revive();
        assertTrue(hero.isAlive());
        assertEquals(hero.getCurrentMaxHealth(), hero.getCurrentHealth());
        assertEquals(hero.getCurrentMaxMana(),   hero.getCurrentMana());
    }
}

