import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WarSimulation {
    private static final int UNIT_COUNT = 30;
    private static final int TURNS = 30;
    private static final Random RANDOM = new Random(20261001L);

    private WarSimulation() {
    }

    public static void main(String[] args) {
        List<BattleZone> zones = createZones();
        List<AIUnit> units = createUnits(zones);

        System.out.println("=== 三方陣營戰區控制模擬 ===");
        System.out.println("抽象回合制模擬，不代表真實戰術；每 3 回合列出戰況。");
        printSituation(0, units, zones);

        int completedTurns = 0;
        for (int turn = 1; turn <= TURNS; turn++) {
            completedTurns = turn;
            Collections.shuffle(units, RANDOM);
            List<String> events = new ArrayList<>();
            for (AIUnit unit : units) {
                String event = unit.takeTurn(units, RANDOM);
                if (event != null) {
                    events.add(event);
                }
            }

            for (BattleZone zone : zones) {
                zone.updateOccupants(units);
                Faction capturedBy = zone.updateControl();
                if (capturedBy != null) {
                    events.add("【佔領】" + capturedBy.getDisplayName()
                            + " 控制了 " + zone.getName() + "！");
                }
            }

            if (turn % 3 == 0 || turn == TURNS) {
                System.out.println("\n--- 第 " + turn + " 回合 ---");
                printEvents(events, 8);
                printSituation(turn, units, zones);
            }

            if (countAlive(units) == 0) {
                System.out.println("\n所有單位均已退出模擬，提前結束。");
                break;
            }
        }

        System.out.println("\n=== 模擬結束 ===");
        printSituation(completedTurns, units, zones);
    }

    private static List<BattleZone> createZones() {
        List<BattleZone> zones = new ArrayList<>();
        zones.add(new BattleZone("北部補給站", -18, 0, 7));
        zones.add(new BattleZone("中央通訊塔", 0, 0, 7));
        zones.add(new BattleZone("南部能源區", 18, 0, 7));
        return zones;
    }

    private static List<AIUnit> createUnits(List<BattleZone> zones) {
        List<AIUnit> units = new ArrayList<>();
        Faction[] factions = Faction.values();
        int id = 0;
        for (Faction faction : factions) {
            BattleZone home = zones.get(faction.ordinal());
            for (int i = 0; i < UNIT_COUNT / factions.length; i++) {
                double angle = 2.0 * Math.PI * i / (UNIT_COUNT / factions.length);
                double radius = 1.0 + RANDOM.nextDouble() * 3.5;
                double x = home.getX() + Math.cos(angle) * radius;
                double y = home.getY() + Math.sin(angle) * radius;

                BattleZone target = chooseTarget(zones, home);
                units.add(new AIUnit(id++, faction, x, y, target));
            }
        }
        return units;
    }

    private static BattleZone chooseTarget(List<BattleZone> zones, BattleZone home) {
        List<BattleZone> choices = new ArrayList<>(zones);
        choices.remove(home);
        return choices.get(RANDOM.nextInt(choices.size()));
    }

    private static void printEvents(List<String> events, int limit) {
        if (events.isEmpty()) {
            System.out.println("本回合沒有交戰或佔領事件。");
            return;
        }
        int shown = Math.min(limit, events.size());
        for (int i = 0; i < shown; i++) {
            System.out.println("• " + events.get(i));
        }
        if (events.size() > shown) {
            System.out.println("…另有 " + (events.size() - shown) + " 次交戰事件。");
        }
    }

    private static void printSituation(int turn, List<AIUnit> units, List<BattleZone> zones) {
        EnumMap<Faction, Integer> aliveCounts = new EnumMap<>(Faction.class);
        EnumMap<Faction, EnumMap<AIUnit.State, Integer>> stateCounts =
                new EnumMap<>(Faction.class);
        for (Faction faction : Faction.values()) {
            aliveCounts.put(faction, 0);
            stateCounts.put(faction, new EnumMap<AIUnit.State, Integer>(AIUnit.State.class));
            for (AIUnit.State state : AIUnit.State.values()) {
                stateCounts.get(faction).put(state, 0);
            }
        }
        for (AIUnit unit : units) {
            if (unit.isAlive()) {
                aliveCounts.put(unit.getFaction(), aliveCounts.get(unit.getFaction()) + 1);
                EnumMap<AIUnit.State, Integer> counts = stateCounts.get(unit.getFaction());
                counts.put(unit.getState(), counts.get(unit.getState()) + 1);
            }
        }

        System.out.println("第 " + turn + " 回合 | 存活兵力：紅隊 "
                + aliveCounts.get(Faction.RED) + " | 藍隊 " + aliveCounts.get(Faction.BLUE)
                + " | 綠隊 " + aliveCounts.get(Faction.GREEN));
        for (Faction faction : Faction.values()) {
            EnumMap<AIUnit.State, Integer> counts = stateCounts.get(faction);
            System.out.println(faction.getDisplayName() + " 狀態：巡邏 "
                    + counts.get(AIUnit.State.PATROLLING) + " | 交戰 "
                    + counts.get(AIUnit.State.ENGAGING) + " | 佔領 "
                    + counts.get(AIUnit.State.CAPTURING));
        }
        for (BattleZone zone : zones) {
            String owner = zone.getOwner() == null
                    ? "未控制" : zone.getOwner().getDisplayName();
            System.out.println(zone.getName() + "（" + owner + "）"
                    + " | " + zone.describeOccupants()
                    + " | 熱區內共 " + zone.getTotalUnits() + " 個單位");
        }
    }

    private static int countAlive(List<AIUnit> units) {
        int count = 0;
        for (AIUnit unit : units) {
            if (unit.isAlive()) {
                count++;
            }
        }
        return count;
    }
}
