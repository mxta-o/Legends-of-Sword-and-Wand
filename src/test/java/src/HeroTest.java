package src;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HeroTest {
    @Test
    public void testLevelUpAndHybridization() {
        Hero hero = new Hero("TestHero", HeroClass.WARRIOR);
        for (int i = 1; i < 5; i++) hero.levelUp(HeroClass.WARRIOR);
        assertEquals(HeroClass.WARRIOR, hero.getSpecializationClass());
        for (int i = 0; i < 5; i++) hero.levelUp(HeroClass.MAGE);
        assertEquals(HeroClass.MAGE, hero.getHybridClass());
    }
}
