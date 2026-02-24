package src;
import java.util.List;

public abstract class Ability {
    protected String name;
    protected int manaCost;

    public Ability(String name, int manaCost) {
        this.name = name;
        this.manaCost = manaCost;
    }

    public String getName() {
        return name;
    }

    public int getManaCost() {
        return manaCost;
    }

    public abstract void execute(Hero caster, List<Hero> targets);
}
