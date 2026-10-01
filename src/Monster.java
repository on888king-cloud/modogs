import java.util.Random;

public class Monster {
    private static final String[] NAMES = {"史萊姆", "森林狼", "哥布林", "洞穴蝙蝠", "骷髏戰士"};
    private static final int[] HEALTH_VALUES = {35, 50, 45, 30, 60};
    private static final int[] ATTACK_VALUES = {8, 12, 10, 7, 14};

    private final String name;
    private final int maxHealth;
    private final int attackPower;
    private int health;

    public Monster(String name, int health, int attackPower) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("怪物名稱不可為空");
        }
        if (health <= 0 || attackPower < 0) {
            throw new IllegalArgumentException("怪物生命值必須大於零，攻擊力不可小於零");
        }
        this.name = name;
        this.maxHealth = health;
        this.health = health;
        this.attackPower = attackPower;
    }

    public static Monster randomMonster(Random random) {
        int index = random.nextInt(NAMES.length);
        int health = HEALTH_VALUES[index] + random.nextInt(11);
        int attack = ATTACK_VALUES[index] + random.nextInt(4);
        return new Monster(NAMES[index], health, attack);
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public int attack() {
        return attackPower;
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - Math.max(0, damage));
    }
}
