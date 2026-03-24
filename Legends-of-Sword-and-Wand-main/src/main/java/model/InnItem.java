package model;

/**
 * Items available for purchase at an Inn, as specified in the game description.
 */
public enum InnItem {

    BREAD  (200,  "Bread",  ItemType.FOOD,  20,  0),
    CHEESE (500,  "Cheese", ItemType.FOOD,  50,  0),
    STEAK  (1000, "Steak",  ItemType.FOOD,  200, 0),
    WATER  (150,  "Water",  ItemType.DRINK, 0,   10),
    JUICE  (400,  "Juice",  ItemType.DRINK, 0,   30),
    WINE   (750,  "Wine",   ItemType.DRINK, 0,   100),
    ELIXIR (2000, "Elixir", ItemType.ELIXIR, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int cost;
    private final String displayName;
    private final ItemType type;
    private final int hpRestore;
    private final int manaRestore;

    InnItem(int cost, String displayName, ItemType type, int hpRestore, int manaRestore) {
        this.cost        = cost;
        this.displayName = displayName;
        this.type        = type;
        this.hpRestore   = hpRestore;
        this.manaRestore = manaRestore;
    }

    public int    getCost()        { return cost; }
    public String getDisplayName() { return displayName; }
    public ItemType getType()      { return type; }
    public int    getHpRestore()   { return hpRestore; }
    public int    getManaRestore() { return manaRestore; }

    /**
     * Applies this item's effect to the given hero.
     * Elixir fully revives, heals and restores mana.
     */
    public void applyTo(Hero hero) {
        if (type == ItemType.ELIXIR) {
            hero.revive();
        } else {
            if (hpRestore > 0)   hero.heal(hpRestore);
            if (manaRestore > 0) hero.restoreMana(manaRestore);
        }
    }

    public enum ItemType { FOOD, DRINK, ELIXIR }

    @Override
    public String toString() {
        if (type == ItemType.ELIXIR) {
            return String.format("%s (%dg) — Revive + Full restore", displayName, cost);
        }
        if (hpRestore > 0) {
            return String.format("%s (%dg) — +%d HP", displayName, cost, hpRestore);
        }
        return String.format("%s (%dg) — +%d mana", displayName, cost, manaRestore);
    }
}
