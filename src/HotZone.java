import java.util.Random;

public final class HotZone {
    private final double movementRadius;
    private final double radius;
    private double x;
    private double y;
    private double targetX;
    private double targetY;

    public HotZone(double x, double y, double movementRadius, double radius) {
        if (movementRadius <= 0 || radius <= 0) {
            throw new IllegalArgumentException("熱區範圍必須大於零");
        }
        this.x = x;
        this.y = y;
        this.movementRadius = movementRadius;
        this.radius = radius;
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

    public void moveRandomly(Random random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * movementRadius;
        targetX = Math.cos(angle) * distance;
        targetY = Math.sin(angle) * distance;
    }

    public void moveOneSecond() {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double step = Math.min(0.65, distance);
        if (distance > 0) {
            x += dx / distance * step;
            y += dy / distance * step;
        }
    }

    public boolean contains(double unitX, double unitY) {
        double dx = unitX - x;
        double dy = unitY - y;
        return dx * dx + dy * dy <= radius * radius;
    }
}
