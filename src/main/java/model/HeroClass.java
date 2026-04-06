package model;

import model.heroclass.ChaosStrategy;
import model.HeroClassStrategy;
import model.heroclass.MageStrategy;
import model.heroclass.OrderStrategy;
import model.heroclass.WarriorStrategy;

public enum HeroClass {
    ORDER,
    CHAOS,
    WARRIOR,
    MAGE,
    HYBRID;

    public HeroClassStrategy createStrategy() {
        switch (this) {
            case ORDER:   return new OrderStrategy();
            case CHAOS:   return new ChaosStrategy();
            case WARRIOR: return new WarriorStrategy();
            case MAGE:    return new MageStrategy();
            default:      return new WarriorStrategy();
        }
    }
}
