public enum Faction {
    RED("紅隊"),
    BLUE("藍隊"),
    GREEN("綠隊");

    private final String displayName;

    Faction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
