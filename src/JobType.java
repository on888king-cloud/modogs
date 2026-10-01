public enum JobType {
    WARRIOR("戰士", 1.8f, 22, 1.05f, 3.2f, 140, 4f),
    MAGE("法師", 10f, 16, 2.5f, 3.0f, 75, 0.3f),
    ARCHER("弓箭手", 15f, 8, 1.3f, 3.4f, 80, 0f),
    ENGINEER("工兵", 2.8f, 11, 0.85f, 2.4f, 115, 0f),
    LOGISTICS("運輸兵", 2.5f, 8, 1.0f, 3.0f, 95, 0f),
    ROGUE("盜賊", 2.2f, 18, 0.75f, 4.8f, 80, 2.8f),
    PRIEST("牧師", 9f, 7, 3.0f, 2.9f, 90, 0f);

    private final String displayName;
    private final float attackRange;
    private final int damage;
    private final float attackCooldown;
    private final float movementSpeed;
    private final int maxHealth;
    private final float knockback;

    JobType(String displayName, float attackRange, int damage, float attackCooldown,
            float movementSpeed, int maxHealth, float knockback) {
        this.displayName = displayName;
        this.attackRange = attackRange;
        this.damage = damage;
        this.attackCooldown = attackCooldown;
        this.movementSpeed = movementSpeed;
        this.maxHealth = maxHealth;
        this.knockback = knockback;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public int getDamage() {
        return damage;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public float getKnockback() {
        return knockback;
    }
}
