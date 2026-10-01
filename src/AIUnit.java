import java.util.List;
import java.util.Random;

public class AIUnit {
    public enum State {
        PATROLLING("巡邏"),
        ENGAGING("交戰"),
        CAPTURING("佔領熱區");

        private final String displayName;

        State(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final double MOVE_SPEED = 2.2;
    private static final double ATTACK_RANGE = 3.0;

    private final int id;
    private final Faction faction;
    private int health = 100;
    private double x;
    private double y;
    private BattleZone targetZone;
    private State state = State.PATROLLING;

    public AIUnit(int id, Faction faction, double x, double y, BattleZone targetZone) {
        if (id < 0 || faction == null || targetZone == null) {
            throw new IllegalArgumentException("AI 單位資料無效");
        }
        this.id = id;
        this.faction = faction;
        this.x = x;
        this.y = y;
        this.targetZone = targetZone;
    }

    public int getId() {
        return id;
    }

    public Faction getFaction() {
        return faction;
    }

    public int getHealth() {
        return health;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public State getState() {
        return state;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public String takeTurn(List<AIUnit> units, Random random) {
        if (!isAlive()) {
            return null;
        }

        AIUnit target = findEnemyInRange(units);
        if (target != null) {
            state = State.ENGAGING;
            int damage = 10 + random.nextInt(16);
            target.health = Math.max(0, target.health - damage);
            return "AI-" + id + "（" + faction.getDisplayName() + "）攻擊 AI-"
                    + target.id + "，造成 " + damage + " 傷害"
                    + (target.isAlive() ? "，目標剩餘 HP " + target.health : "，目標退出戰區");
        }

        double dx = targetZone.getX() - x;
        double dy = targetZone.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > targetZone.getRadius() * 0.72) {
            state = State.PATROLLING;
            double step = Math.min(MOVE_SPEED, distance);
            x += dx / distance * step;
            y += dy / distance * step;
            return null;
        }

        state = State.CAPTURING;
        target = findEnemyInRange(units);
        if (target != null) {
            state = State.ENGAGING;
            int damage = 10 + random.nextInt(16);
            target.health = Math.max(0, target.health - damage);
            return "AI-" + id + "（" + faction.getDisplayName() + "）守住熱區並攻擊 AI-"
                    + target.id + "，造成 " + damage + " 傷害";
        }
        return null;
    }

    private AIUnit findEnemyInRange(List<AIUnit> units) {
        AIUnit closest = null;
        double closestDistance = ATTACK_RANGE;
        for (AIUnit candidate : units) {
            if (!candidate.isAlive() || candidate.faction == faction) {
                continue;
            }
            double distance = distanceTo(candidate.x, candidate.y);
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    private double distanceTo(double otherX, double otherY) {
        double dx = otherX - x;
        double dy = otherY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
