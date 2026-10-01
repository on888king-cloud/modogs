import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class BattleZone {
    private final String name;
    private final double x;
    private final double y;
    private final double radius;
    private final EnumMap<Faction, Integer> unitCounts = new EnumMap<>(Faction.class);
    private final EnumMap<Faction, Integer> captureProgress = new EnumMap<>(Faction.class);
    private Faction owner;

    public BattleZone(String name, double x, double y, double radius) {
        if (name == null || name.trim().isEmpty() || radius <= 0) {
            throw new IllegalArgumentException("戰區名稱不可為空，範圍必須大於零");
        }
        this.name = name;
        this.x = x;
        this.y = y;
        this.radius = radius;
        for (Faction faction : Faction.values()) {
            unitCounts.put(faction, 0);
            captureProgress.put(faction, 0);
        }
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    public Faction getOwner() {
        return owner;
    }

    public int getUnitCount(Faction faction) {
        return unitCounts.get(faction);
    }

    public Map<Faction, Integer> getUnitCounts() {
        return Collections.unmodifiableMap(new EnumMap<>(unitCounts));
    }

    public void updateOccupants(Iterable<AIUnit> units) {
        for (Faction faction : Faction.values()) {
            unitCounts.put(faction, 0);
        }
        for (AIUnit unit : units) {
            if (unit.isAlive() && contains(unit.getX(), unit.getY())) {
                unitCounts.put(unit.getFaction(), unitCounts.get(unit.getFaction()) + 1);
            }
        }
    }

    public Faction updateControl() {
        int presentFactions = 0;
        Faction onlyFaction = null;
        for (Faction faction : Faction.values()) {
            if (unitCounts.get(faction) > 0) {
                presentFactions++;
                onlyFaction = faction;
            }
        }

        if (presentFactions == 1) {
            for (Faction faction : Faction.values()) {
                int progress = captureProgress.get(faction);
                captureProgress.put(faction, faction == onlyFaction ? progress + 1 : 0);
            }
            if (captureProgress.get(onlyFaction) >= 3 && owner != onlyFaction) {
                owner = onlyFaction;
                for (Faction faction : Faction.values()) {
                    captureProgress.put(faction, 0);
                }
                return owner;
            }
        } else if (presentFactions == 0) {
            for (Faction faction : Faction.values()) {
                captureProgress.put(faction, Math.max(0, captureProgress.get(faction) - 1));
            }
        }
        return null;
    }

    public boolean contains(double pointX, double pointY) {
        double dx = pointX - x;
        double dy = pointY - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    public int getTotalUnits() {
        int total = 0;
        for (int count : unitCounts.values()) {
            total += count;
        }
        return total;
    }

    public String describeOccupants() {
        StringBuilder result = new StringBuilder();
        for (Faction faction : Faction.values()) {
            if (result.length() > 0) {
                result.append("、");
            }
            result.append(faction.getDisplayName()).append(' ').append(unitCounts.get(faction));
        }
        return result.toString();
    }
}
