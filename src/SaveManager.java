import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class SaveManager {
    private static final String SAVE_VERSION = "1";

    private SaveManager() {
    }

    public static void save(Player player, Path file) throws IOException {
        Properties data = new Properties();
        data.setProperty("version", SAVE_VERSION);
        data.setProperty("health", Integer.toString(player.getHealth()));
        data.setProperty("coins", Integer.toString(player.getCoins()));
        data.setProperty("weaponName", player.getWeaponName());
        data.setProperty("weaponBonus", Integer.toString(player.getWeaponBonus()));

        List<String> inventory = player.getInventory();
        data.setProperty("inventory.count", Integer.toString(inventory.size()));
        for (int i = 0; i < inventory.size(); i++) {
            data.setProperty("inventory." + i, inventory.get(i));
        }

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            data.store(writer, "Text RPG save data");
        }
    }

    public static void load(Player player, Path file) throws IOException {
        Properties data = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
        }

        if (!SAVE_VERSION.equals(data.getProperty("version"))) {
            throw new IOException("不支援此存檔版本。");
        }

        try {
            int health = parseInt(data, "health");
            int coins = parseInt(data, "coins");
            String weaponName = required(data, "weaponName");
            int weaponBonus = parseInt(data, "weaponBonus");
            int inventoryCount = parseInt(data, "inventory.count");
            if (inventoryCount < 0 || inventoryCount > 1000) {
                throw new IOException("存檔中的背包項目數量無效。");
            }

            List<String> inventory = new ArrayList<>();
            for (int i = 0; i < inventoryCount; i++) {
                inventory.add(required(data, "inventory." + i));
            }
            player.restoreProgress(health, coins, weaponName, weaponBonus, inventory);
        } catch (IllegalArgumentException exception) {
            throw new IOException("存檔內容無效：" + exception.getMessage(), exception);
        }
    }

    private static int parseInt(Properties data, String key) throws IOException {
        return Integer.parseInt(required(data, key));
    }

    private static String required(Properties data, String key) throws IOException {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IOException("存檔缺少必要欄位：" + key);
        }
        return value;
    }
}
