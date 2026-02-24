package src;

public class StatusEffect {
    private StatusType type;
    private int duration;

    public StatusEffect(StatusType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    public void apply(Hero hero) {
        // Implement effect logic (e.g., stun disables action, shield absorbs damage)
    }

    public void tick() {
        duration--;
    }

    public boolean isExpired() {
        return duration <= 0;
    }

    public StatusType getType() {
        return type;
    }
}
