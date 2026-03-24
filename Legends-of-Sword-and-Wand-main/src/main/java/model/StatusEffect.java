package model;

public class StatusEffect {
    private StatusType type;
    private int duration;
    private int magnitude; // e.g., shield amount for SHIELD type

    public StatusEffect(StatusType type, int duration) {
        this(type, duration, 0);
    }

    public StatusEffect(StatusType type, int duration, int magnitude) {
        this.type = type;
        this.duration = duration;
        this.magnitude = magnitude;
    }

    public void apply(Hero hero) {
        switch (type) {
            case STUN:
                hero.setStunned(true);
                break;
            case SHIELD:
                hero.addShield(magnitude);
                break;
        }
    }

    /** Called at the end of each turn. Removes stun when expired. */
    public void tick() {
        duration--;
    }

    public void expire(Hero hero) {
        if (type == StatusType.STUN) {
            hero.setStunned(false);
        }
        // Shield amount is tracked directly on Hero; nothing extra needed here.
    }

    public boolean isExpired() {
        return duration <= 0;
    }

    public StatusType getType() {
        return type;
    }

    public int getMagnitude() {
        return magnitude;
    }
}
