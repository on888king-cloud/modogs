import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private static final int MAX_HEALTH = 100;
    private static final int BASE_ATTACK_POWER = 20;
    private static final int POTION_HEAL = 40;

    private final String name = "冒險者";
    private final List<String> inventory = new ArrayList<>();
    private int health = MAX_HEALTH;
    private int coins;
    private String weaponName = "普通長劍";
    private int weaponBonus;

    public Player() {
        reset();
    }

    public void reset() {
        health = MAX_HEALTH;
        coins = 0;
        weaponName = "普通長劍";
        weaponBonus = 0;
        inventory.clear();
        inventory.add("生命藥水");
        inventory.add(weaponName);
    }

    public void revive() {
        health = MAX_HEALTH;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public int getAttackPower() {
        return BASE_ATTACK_POWER + weaponBonus;
    }

    public int getPotionCount() {
        return countPotions();
    }

    public int getCoins() {
        return coins;
    }

    public String getWeaponName() {
        return weaponName;
    }

    public int getWeaponBonus() {
        return weaponBonus;
    }

    public String getInventorySummary() {
        return String.join("、", inventory);
    }

    public List<String> getInventory() {
        return Collections.unmodifiableList(new ArrayList<>(inventory));
    }

    public boolean isAlive() {
        return health > 0;
    }

    public int attack() {
        return getAttackPower();
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - Math.max(0, damage));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    public int usePotion() {
        if (countPotions() == 0 || health == MAX_HEALTH) {
            return 0;
        }
        inventory.remove("生命藥水");
        int previousHealth = health;
        health = Math.min(MAX_HEALTH, health + POTION_HEAL);
        return health - previousHealth;
    }

    public void addPotions(int amount) {
        for (int i = 0; i < Math.max(0, amount); i++) {
            inventory.add("生命藥水");
        }
    }

    public boolean equipWeapon(String name, int bonus) {
        if (bonus <= weaponBonus) {
            return false;
        }
        inventory.remove(weaponName);
        weaponName = name;
        weaponBonus = bonus;
        inventory.add(weaponName);
        return true;
    }

    public void addCoins(int amount) {
        coins += Math.max(0, amount);
    }

    public boolean spendCoins(int amount) {
        if (amount < 0 || coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    public void restoreProgress(int restoredHealth, int restoredCoins, String restoredWeaponName,
                               int restoredWeaponBonus, List<String> restoredInventory) {
        if (restoredHealth < 0 || restoredHealth > MAX_HEALTH
                || restoredCoins < 0 || restoredWeaponName == null || restoredWeaponName.isEmpty()
                || restoredWeaponBonus < 0 || restoredInventory == null
                || !restoredInventory.contains(restoredWeaponName)) {
            throw new IllegalArgumentException("存檔中的玩家資料無效");
        }
        for (String item : restoredInventory) {
            if (item == null || item.isEmpty()) {
                throw new IllegalArgumentException("存檔中的背包資料無效");
            }
        }

        health = restoredHealth;
        coins = restoredCoins;
        weaponName = restoredWeaponName;
        weaponBonus = restoredWeaponBonus;
        inventory.clear();
        inventory.addAll(restoredInventory);
    }

    private int countPotions() {
        int count = 0;
        for (String item : inventory) {
            if (item.equals("生命藥水")) {
                count++;
            }
        }
        return count;
    }
}
