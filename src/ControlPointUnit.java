import java.util.Random;

public final class ControlPointUnit {
    private static final double MOVE_DISTANCE_PER_SECOND = 1.8;
    private static final int MAX_HEALTH = 100;
    private static final int RESPAWN_SECONDS = 5;

    private final int id;
    private final Faction faction;
    private double x;
    private double y;
    private int health = MAX_HEALTH;
    private int respawnSecondsRemaining;
    private int attackCooldown;

    public ControlPointUnit(int id, Faction faction, double x, double y) {
        if (id < 0 || faction == null) {
            throw new IllegalArgumentException("單位資料無效");
        }
        this.id = id;
        this.faction = faction;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public Faction getFaction() {
        return faction;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isRespawning() {
        return !isAlive() && respawnSecondsRemaining > 0;
    }

    public int getRespawnSecondsRemaining() {
        return respawnSecondsRemaining;
    }

    public boolean canAttack() {
        return isAlive() && attackCooldown <= 0;
    }

    public void moveToward(double targetX, double targetY, double mapLimit, Random random) {
        if (!isAlive()) {
            return;
        }
        double dx = targetX - x + (random.nextDouble() * 2.0 - 1.0) * 0.8;
        double dy = targetY - y + (random.nextDouble() * 2.0 - 1.0) * 0.8;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 0.05) {
            double step = Math.min(MOVE_DISTANCE_PER_SECOND, distance);
            x += dx / distance * step;
            y += dy / distance * step;
        }
        x = Math.max(-mapLimit, Math.min(mapLimit, x));
        y = Math.max(-mapLimit, Math.min(mapLimit, y));
    }

    public void takeDamage(int damage) {
        if (!isAlive()) {
            return;
        }
        health = Math.max(0, health - Math.max(0, damage));
        if (health == 0) {
            respawnSecondsRemaining = RESPAWN_SECONDS;
            attackCooldown = 0;
        }
    }

    public void tickRespawn(int spawnX, int spawnY) {
        if (isAlive()) {
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            return;
        }
        respawnSecondsRemaining--;
        if (respawnSecondsRemaining <= 0) {
            health = MAX_HEALTH;
            x = spawnX;
            y = spawnY;
            attackCooldown = 1;
        }
    }

    public void startAttackCooldown() {
        attackCooldown = 2;
    }
}
