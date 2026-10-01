public final class ControlArea {
    private final String name;
    private final double centerX;
    private final double centerY;
    private final double radius;
    private final double mapLimit;

    public ControlArea(String name, double centerX, double centerY, double radius) {
        this(name, centerX, centerY, radius, 25);
    }

    public ControlArea(String name, double centerX, double centerY, double radius, double mapLimit) {
        if (name == null || name.trim().isEmpty() || radius <= 0) {
            throw new IllegalArgumentException("控制區資料無效");
        }
        if (mapLimit <= 0 || radius > mapLimit * Math.sqrt(2.0)) {
            throw new IllegalArgumentException("地圖範圍無效");
        }
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.mapLimit = mapLimit;
    }

    public String getName() {
        return name;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getRadius() {
        return radius;
    }

    public double getMapLimit() {
        return mapLimit;
    }

    public boolean contains(double x, double y) {
        double dx = x - centerX;
        double dy = y - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }
}
