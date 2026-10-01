import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

public final class KingOfTheHillSimulation {
    private static final int UNITS_PER_FACTION = 10;
    private static final int TARGET_SCORE = 100;
    private static final int SCORE_INTERVAL_SECONDS = 30;
    private static final int HOT_ZONE_TARGET_INTERVAL_SECONDS = 60;
    private static final int MAP_PRINT_INTERVAL_SECONDS = 5;
    private static final int MAP_SIZE = 50;
    private static final double COMBAT_RANGE = 2.0;
    private static final int MAX_EVENTS_PER_UPDATE = 8;
    private static final Random RANDOM = new Random(20261001L);

    private KingOfTheHillSimulation() {
    }

    public static void main(String[] args) throws InterruptedException {
        boolean fastMode = containsArgument(args, "--fast");
        boolean quietMode = containsArgument(args, "--quiet");
        int maxSeconds = readMaxSeconds(args);
        runSimulation(fastMode, quietMode, maxSeconds);
    }

    private static void runSimulation(boolean fastMode, boolean quietMode, int maxSeconds)
            throws InterruptedException {
        ControlArea controlArea = new ControlArea("中央控制區", 0, 0, 24, 25);
        HotZone hotZone = new HotZone(0, 0, 16, 5);
        EnumMap<Faction, Integer> scores = new EnumMap<>(Faction.class);
        EnumMap<Faction, Integer> weightedCounts = new EnumMap<>(Faction.class);
        EnumMap<Faction, Integer> respawnPointsX = new EnumMap<>(Faction.class);
        EnumMap<Faction, Integer> respawnPointsY = new EnumMap<>(Faction.class);
        List<ControlPointUnit> units = createUnits(hotZone, respawnPointsX, respawnPointsY);

        for (Faction faction : Faction.values()) {
            scores.put(faction, 0);
            weightedCounts.put(faction, 0);
        }

        System.out.println("=== 山丘之王：三方戰區模擬 ===");
        System.out.println("30 個 AI（每陣營 10 個），每秒更新；陣亡 5 秒後於本陣重生。");
        System.out.println("每 30 秒結算：控制區內每人 1 票、熱區內每人 2 票；先達 "
                + TARGET_SCORE + " 分獲勝。");
        System.out.println("小地圖符號：R 紅隊、G 綠隊、B 藍隊、x 陣亡位置、@ 熱區中心。");

        int gameSeconds = 0;
        printStatus(gameSeconds, controlArea, hotZone, units, weightedCounts, scores,
                quietMode);

        for (gameSeconds = 1; ; gameSeconds++) {
            if (!fastMode) {
                Thread.sleep(1000L);
            }

            hotZone.moveOneSecond();
            if (gameSeconds % HOT_ZONE_TARGET_INTERVAL_SECONDS == 0) {
                hotZone.moveRandomly(RANDOM);
            }

            List<String> events = updateUnits(units, hotZone, controlArea,
                    respawnPointsX, respawnPointsY);

            if (gameSeconds % SCORE_INTERVAL_SECONDS == 0) {
                countWeightedUnits(controlArea, hotZone, units, weightedCounts);
                Faction scoringFaction = determineRoundWinner(weightedCounts);
                if (scoringFaction != null) {
                    scores.put(scoringFaction, scores.get(scoringFaction) + 1);
                    events.add(scoringFaction.getDisplayName()
                            + " 控制戰區，獲得 1 分。");
                } else {
                    events.add("戰區票數平手，本次不計分。");
                }

                printStatus(gameSeconds, controlArea, hotZone, units,
                        weightedCounts, scores, quietMode, events);

                if (scoringFaction != null
                        && scores.get(scoringFaction) >= TARGET_SCORE) {
                    System.out.println("\n【遊戲結束】" + scoringFaction.getDisplayName()
                            + " 陣營率先達到 " + TARGET_SCORE + " 分獲勝！");
                    printFinalScores(scores, units);
                    return;
                }
            } else if (!quietMode && gameSeconds % MAP_PRINT_INTERVAL_SECONDS == 0) {
                printStatus(gameSeconds, controlArea, hotZone, units,
                        weightedCounts, scores, false, events);
            }

            if (maxSeconds > 0 && gameSeconds >= maxSeconds) {
                System.out.println("\n【測試停止】已達指定模擬時間 " + maxSeconds
                        + " 秒，尚未有陣營達到 " + TARGET_SCORE + " 分。");
                printFinalScores(scores, units);
                return;
            }
        }
    }

    private static List<ControlPointUnit> createUnits(HotZone hotZone,
            EnumMap<Faction, Integer> spawnX, EnumMap<Faction, Integer> spawnY) {
        List<ControlPointUnit> units = new ArrayList<>();
        spawnX.put(Faction.RED, -23);
        spawnY.put(Faction.RED, 0);
        spawnX.put(Faction.GREEN, 0);
        spawnY.put(Faction.GREEN, 23);
        spawnX.put(Faction.BLUE, 23);
        spawnY.put(Faction.BLUE, 0);

        int id = 0;
        for (Faction faction : Faction.values()) {
            int baseX = spawnX.get(faction);
            int baseY = spawnY.get(faction);
            for (int i = 0; i < UNITS_PER_FACTION; i++) {
                double angle = Math.PI * 2.0 * i / UNITS_PER_FACTION;
                double radius = 1.0 + RANDOM.nextDouble() * 3.0;
                units.add(new ControlPointUnit(id++, faction,
                        baseX + Math.cos(angle) * radius,
                        baseY + Math.sin(angle) * radius));
            }
        }
        hotZone.moveRandomly(RANDOM);
        return units;
    }

    private static List<String> updateUnits(List<ControlPointUnit> units, HotZone hotZone,
            ControlArea controlArea, EnumMap<Faction, Integer> spawnX,
            EnumMap<Faction, Integer> spawnY) {
        List<String> events = new ArrayList<>();
        for (ControlPointUnit unit : units) {
            if (!unit.isAlive()) {
                int remaining = unit.getRespawnSecondsRemaining();
                unit.tickRespawn(spawnX.get(unit.getFaction()), spawnY.get(unit.getFaction()));
                if (remaining == 1 && unit.isAlive()) {
                    events.add(unit.getFaction().getDisplayName() + " AI-" + unit.getId()
                            + " 正在重生。");
                }
                continue;
            }
            unit.tickRespawn(spawnX.get(unit.getFaction()), spawnY.get(unit.getFaction()));
            unit.moveToward(hotZone.getX(), hotZone.getY(),
                    controlArea.getMapLimit(), RANDOM);
        }

        Collections.shuffle(units, RANDOM);
        for (ControlPointUnit attacker : units) {
            if (!attacker.canAttack()) {
                continue;
            }
            ControlPointUnit target = findEnemyInRange(attacker, units);
            if (target == null) {
                continue;
            }

            int damage = 12 + RANDOM.nextInt(17);
            target.takeDamage(damage);
            attacker.startAttackCooldown();
            if (events.size() < MAX_EVENTS_PER_UPDATE) {
                events.add(attacker.getFaction().getDisplayName() + " AI-" + attacker.getId()
                        + " 攻擊 " + target.getFaction().getDisplayName() + " AI-"
                        + target.getId() + "，造成 " + damage + " 傷害。");
            }
            if (!target.isAlive() && events.size() < MAX_EVENTS_PER_UPDATE) {
                events.add(target.getFaction().getDisplayName() + " AI-" + target.getId()
                        + " 陣亡，5 秒後從後方重生。");
            }
        }
        return events;
    }

    private static ControlPointUnit findEnemyInRange(ControlPointUnit attacker,
                                                       List<ControlPointUnit> units) {
        ControlPointUnit closest = null;
        double closestDistance = COMBAT_RANGE;
        for (ControlPointUnit candidate : units) {
            if (!candidate.isAlive() || candidate.getFaction() == attacker.getFaction()) {
                continue;
            }
            double dx = attacker.getX() - candidate.getX();
            double dy = attacker.getY() - candidate.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    private static void countWeightedUnits(ControlArea controlArea, HotZone hotZone,
            List<ControlPointUnit> units, EnumMap<Faction, Integer> counts) {
        for (Faction faction : Faction.values()) {
            counts.put(faction, 0);
        }
        for (ControlPointUnit unit : units) {
            if (!unit.isAlive() || !controlArea.contains(unit.getX(), unit.getY())) {
                continue;
            }
            int weight = hotZone.contains(unit.getX(), unit.getY()) ? 2 : 1;
            counts.put(unit.getFaction(), counts.get(unit.getFaction()) + weight);
        }
    }

    private static Faction determineRoundWinner(EnumMap<Faction, Integer> counts) {
        Faction winner = null;
        int highest = 0;
        boolean tied = false;
        for (Faction faction : Faction.values()) {
            int count = counts.get(faction);
            if (count > highest) {
                highest = count;
                winner = faction;
                tied = false;
            } else if (count == highest && count > 0) {
                tied = true;
            }
        }
        return highest == 0 || tied ? null : winner;
    }

    private static void printStatus(int seconds, ControlArea controlArea, HotZone hotZone,
            List<ControlPointUnit> units, EnumMap<Faction, Integer> counts,
            EnumMap<Faction, Integer> scores, boolean quietMode) {
        printStatus(seconds, controlArea, hotZone, units, counts, scores, quietMode,
                Collections.<String>emptyList());
    }

    private static void printStatus(int seconds, ControlArea controlArea, HotZone hotZone,
            List<ControlPointUnit> units, EnumMap<Faction, Integer> counts,
            EnumMap<Faction, Integer> scores, boolean quietMode, List<String> events) {
        int alive = 0;
        for (ControlPointUnit unit : units) {
            if (unit.isAlive()) {
                alive++;
            }
        }
        System.out.println("\n=== 戰況 " + formatTime(seconds) + " | 存活 " + alive
                + "/" + units.size() + " | 熱區 (" + Math.round(hotZone.getX())
                + ", " + Math.round(hotZone.getY()) + ") ===");
        System.out.println("比分：紅 " + scores.get(Faction.RED) + "/" + TARGET_SCORE
                + " | 綠 " + scores.get(Faction.GREEN) + "/" + TARGET_SCORE
                + " | 藍 " + scores.get(Faction.BLUE) + "/" + TARGET_SCORE);
        System.out.println("加權票數：紅 " + counts.get(Faction.RED)
                + " | 綠 " + counts.get(Faction.GREEN)
                + " | 藍 " + counts.get(Faction.BLUE));
        printEvents(events);
        if (!quietMode) {
            printMap(controlArea, hotZone, units);
        }
    }

    private static void printEvents(List<String> events) {
        int shown = Math.min(events.size(), MAX_EVENTS_PER_UPDATE);
        for (int i = 0; i < shown; i++) {
            System.out.println("  " + events.get(i));
        }
        if (events.size() > shown) {
            System.out.println("  另有 " + (events.size() - shown) + " 個戰況事件。");
        }
    }

    private static void printMap(ControlArea controlArea, HotZone hotZone,
                                 List<ControlPointUnit> units) {
        char[][] map = new char[MAP_SIZE][MAP_SIZE];
        for (int row = 0; row < MAP_SIZE; row++) {
            for (int column = 0; column < MAP_SIZE; column++) {
                int x = column - MAP_SIZE / 2;
                int y = MAP_SIZE / 2 - row;
                double distanceFromCenter = Math.sqrt(
                        Math.pow(x - controlArea.getCenterX(), 2)
                                + Math.pow(y - controlArea.getCenterY(), 2));
                map[row][column] = distanceFromCenter <= controlArea.getRadius()
                        ? '.' : ' ';
                if (Math.abs(distanceFromCenter - controlArea.getRadius()) < 0.65) {
                    map[row][column] = '+';
                }
                if (hotZone.contains(x, y)) {
                    map[row][column] = 'h';
                }
            }
        }

        int hotX = toColumn(hotZone.getX());
        int hotY = toRow(hotZone.getY());
        map[hotY][hotX] = '@';
        for (ControlPointUnit unit : units) {
            int column = toColumn(unit.getX());
            int row = toRow(unit.getY());
            char symbol = unit.isAlive() ? symbolFor(unit.getFaction()) : 'x';
            char existing = map[row][column];
            if (existing == 'R' || existing == 'G' || existing == 'B') {
                if (existing != symbol) {
                    map[row][column] = '*';
                }
            } else {
                map[row][column] = symbol;
            }
        }

        System.out.println("控制區邊界 + | 熱區 @/h | 陣營 R/G/B | 陣亡位置 x | 多陣營重疊 *");
        for (char[] row : map) {
            System.out.println(new String(row));
        }
    }

    private static int toColumn(double x) {
        return clampIndex((int) Math.round(x) + MAP_SIZE / 2);
    }

    private static int toRow(double y) {
        return clampIndex(MAP_SIZE / 2 - (int) Math.round(y));
    }

    private static int clampIndex(int index) {
        return Math.max(0, Math.min(MAP_SIZE - 1, index));
    }

    private static char symbolFor(Faction faction) {
        if (faction == Faction.RED) {
            return 'R';
        }
        if (faction == Faction.GREEN) {
            return 'G';
        }
        return 'B';
    }

    private static String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static boolean containsArgument(String[] args, String expected) {
        for (String argument : args) {
            if (expected.equals(argument)) {
                return true;
            }
        }
        return false;
    }

    private static int readMaxSeconds(String[] args) {
        String prefix = "--max-seconds=";
        for (String argument : args) {
            if (argument.startsWith(prefix)) {
                int seconds = Integer.parseInt(argument.substring(prefix.length()));
                if (seconds <= 0) {
                    throw new IllegalArgumentException("--max-seconds 必須大於零");
                }
                return seconds;
            }
        }
        return 0;
    }

    private static void printFinalScores(EnumMap<Faction, Integer> scores,
                                         List<ControlPointUnit> units) {
        System.out.println("最終比分：紅隊 " + scores.get(Faction.RED)
                + " | 綠隊 " + scores.get(Faction.GREEN)
                + " | 藍隊 " + scores.get(Faction.BLUE));
        for (Faction faction : Faction.values()) {
            int alive = 0;
            int respawning = 0;
            for (ControlPointUnit unit : units) {
                if (unit.getFaction() == faction) {
                    if (unit.isAlive()) {
                        alive++;
                    } else {
                        respawning++;
                    }
                }
            }
            System.out.println(faction.getDisplayName() + " 存活 " + alive
                    + "，等待重生 " + respawning);
        }
    }
}
