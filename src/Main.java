import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Main {
    private static final Random RANDOM = new Random();
    private static final Path SAVE_FILE = Paths.get("save.txt");
    private static final String[] ROOMS = {
            "迷霧林地", "古老遺跡", "幽暗洞窟", "寂靜溪谷", "荒廢哨站"
    };

    private final Player player = new Player();
    private final JTextArea log = new JTextArea(18, 48);
    private final JProgressBar playerHealthBar = new JProgressBar();
    private final JProgressBar monsterHealthBar = new JProgressBar();
    private final JLabel monsterHealthLabel = new JLabel("目前沒有遭遇怪物");
    private final JButton exploreButton = new JButton("探索");
    private final JButton statusButton = new JButton("查看狀態");
    private final JButton attackButton = new JButton("攻擊");
    private final JButton potionButton = new JButton("使用生命藥水");
    private final JButton fleeButton = new JButton("逃跑");
    private final JButton saveButton = new JButton("存檔");
    private final JButton loadButton = new JButton("讀取存檔");
    private Monster currentMonster;
    private JFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().createWindow());
    }

    private void createWindow() {
        frame = new JFrame("冒險者之旅");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        frame.add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel status = new JPanel(new GridLayout(0, 1, 4, 4));
        playerHealthBar.setStringPainted(true);
        monsterHealthBar.setStringPainted(true);
        status.add(playerHealthBar);
        status.add(monsterHealthLabel);
        status.add(monsterHealthBar);
        frame.add(status, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout());
        actions.add(exploreButton);
        actions.add(statusButton);
        actions.add(attackButton);
        actions.add(potionButton);
        actions.add(fleeButton);
        actions.add(saveButton);
        actions.add(loadButton);
        frame.add(actions, BorderLayout.SOUTH);

        exploreButton.addActionListener(event -> explore());
        statusButton.addActionListener(event -> showStatus());
        attackButton.addActionListener(event -> attack());
        potionButton.addActionListener(event -> usePotion());
        fleeButton.addActionListener(event -> flee());
        saveButton.addActionListener(event -> saveGame());
        loadButton.addActionListener(event -> loadGame());

        append("=== 冒險者之旅 ===");
        try {
            SaveManager.load(player, SAVE_FILE);
            append("已從 " + SAVE_FILE.toAbsolutePath() + " 載入存檔。");
        } catch (NoSuchFileException exception) {
            append("尚無存檔，開始新的冒險。");
        } catch (IOException | IllegalArgumentException exception) {
            append("讀取存檔失敗：" + exception.getMessage());
            append("目前使用新的冒險進度；原存檔未被修改。");
        }
        append("點選「探索」前往隨機生成的房間！");
        updateHealthBars();
        updateActions();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void explore() {
        if (currentMonster != null || !player.isAlive()) {
            return;
        }

        String room = ROOMS[RANDOM.nextInt(ROOMS.length)];
        append("\n你探索了「" + room + "」。");

        int event = RANDOM.nextInt(100);
        if (event < 50) {
            currentMonster = Monster.randomMonster(RANDOM);
            append("遭遇「" + currentMonster.getName() + "」！準備戰鬥。");
        } else if (event < 70) {
            findTreasure();
        } else if (event < 85) {
            int damage = 8 + RANDOM.nextInt(13);
            player.takeDamage(damage);
            append("你觸發了陷阱，受到 " + damage + " 點傷害。");
            checkDefeat();
        } else {
            visitMerchant();
        }
        updateHealthBars();
        updateActions();
    }

    private void findTreasure() {
        if (RANDOM.nextBoolean()) {
            player.addPotions(1);
            append("你在房間裡找到一瓶生命藥水。");
            return;
        }

        String[] prefixes = {"", "鋒利的", "精良的", "傳說之"};
        int[] bonuses = {0, 5, 10, 18};
        int tier = RANDOM.nextInt(prefixes.length);
        String name = prefixes[tier] + "長劍";
        boolean equipped = player.equipWeapon(name, bonuses[tier]);
        append("寶箱中找到「" + name + "」（攻擊力 +" + bonuses[tier] + "）。"
                + (equipped ? "你裝備了新武器！" : "目前武器較強，保留原裝備。"));
    }

    private void visitMerchant() {
        append("你遇到一位旅行商人，他以 5 枚金幣出售生命藥水。");
        if (player.getCoins() >= 5 && javax.swing.JOptionPane.showConfirmDialog(
                frame, "花費 5 枚金幣購買生命藥水嗎？", "旅行商人",
                javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
            player.spendCoins(5);
            player.addPotions(1);
            append("你買下了一瓶生命藥水。");
        } else {
            append(player.getCoins() < 5 ? "你的金幣不足，商人向你揮手道別。" : "你婉拒了交易。");
        }
    }

    private void attack() {
        if (currentMonster == null || !player.isAlive()) {
            return;
        }

        int damage = player.attack();
        currentMonster.takeDamage(damage);
        append("你攻擊 " + currentMonster.getName() + "，造成 " + damage + " 點傷害。");
        if (!currentMonster.isAlive()) {
            append("你擊敗了 " + currentMonster.getName() + "，獲得 3 枚金幣！");
            player.addCoins(3);
            currentMonster = null;
        } else {
            monsterTurn();
        }
        updateHealthBars();
        updateActions();
    }

    private void usePotion() {
        int restored = player.usePotion();
        if (restored == 0) {
            append(player.getPotionCount() == 0
                    ? "背包裡沒有生命藥水！"
                    : "生命值已滿，不需要使用藥水。");
            return;
        }

        append("你使用生命藥水，恢復 " + restored + " 點生命值。");
        if (currentMonster != null) {
            monsterTurn();
        }
        updateHealthBars();
        updateActions();
    }

    private void flee() {
        if (currentMonster == null) {
            return;
        }
        if (RANDOM.nextBoolean()) {
            append("你成功逃離了戰鬥。");
            currentMonster = null;
        } else {
            append("逃跑失敗！");
            monsterTurn();
        }
        updateHealthBars();
        updateActions();
    }

    private void monsterTurn() {
        int damage = currentMonster.attack();
        player.takeDamage(damage);
        append(currentMonster.getName() + " 對你造成 " + damage + " 點傷害。");
        checkDefeat();
    }

    private void checkDefeat() {
        if (!player.isAlive()) {
            currentMonster = null;
            append("你的體力耗盡，冒險就此結束。");
        }
    }

    private void showStatus() {
        append("\n=== 角色狀態 ===");
        append("生命值：" + player.getHealth() + "/" + player.getMaxHealth()
                + " | 攻擊力：" + player.getAttackPower());
        append("武器：" + player.getWeaponName() + " | 金幣：" + player.getCoins());
        append("背包：" + player.getInventorySummary());
    }

    private void saveGame() {
        if (currentMonster != null) {
            append("戰鬥中無法存檔，請先結束戰鬥。");
            return;
        }
        try {
            SaveManager.save(player, SAVE_FILE);
            append("存檔成功：" + SAVE_FILE.toAbsolutePath());
        } catch (IOException exception) {
            append("存檔失敗：" + exception.getMessage());
            javax.swing.JOptionPane.showMessageDialog(frame,
                    "無法寫入存檔：\n" + exception.getMessage(),
                    "存檔失敗", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGame() {
        if (currentMonster != null) {
            append("戰鬥中無法讀取存檔，請先結束戰鬥。");
            return;
        }
        try {
            SaveManager.load(player, SAVE_FILE);
            append("已讀取存檔：" + SAVE_FILE.toAbsolutePath());
            updateHealthBars();
            updateActions();
        } catch (IOException | IllegalArgumentException exception) {
            append("讀取存檔失敗：" + exception.getMessage());
            javax.swing.JOptionPane.showMessageDialog(frame,
                    "無法讀取存檔：\n" + exception.getMessage(),
                    "讀取存檔失敗", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateHealthBars() {
        playerHealthBar.setMaximum(player.getMaxHealth());
        playerHealthBar.setValue(player.getHealth());
        playerHealthBar.setString("玩家生命值：" + player.getHealth() + "/" + player.getMaxHealth());

        boolean inBattle = currentMonster != null;
        monsterHealthLabel.setText(inBattle ? currentMonster.getName() : "目前沒有遭遇怪物");
        monsterHealthBar.setVisible(inBattle);
        if (inBattle) {
            monsterHealthBar.setMaximum(currentMonster.getMaxHealth());
            monsterHealthBar.setValue(currentMonster.getHealth());
            monsterHealthBar.setString("怪物生命值：" + currentMonster.getHealth()
                    + "/" + currentMonster.getMaxHealth());
        }
    }

    private void updateActions() {
        boolean alive = player.isAlive();
        boolean inBattle = currentMonster != null;
        exploreButton.setEnabled(alive && !inBattle);
        statusButton.setEnabled(alive);
        attackButton.setEnabled(alive && inBattle);
        potionButton.setEnabled(alive && player.getPotionCount() > 0
                && player.getHealth() < player.getMaxHealth());
        fleeButton.setEnabled(alive && inBattle);
        saveButton.setEnabled(!inBattle);
        loadButton.setEnabled(!inBattle);
    }

    private void append(String message) {
        log.append(message + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}
