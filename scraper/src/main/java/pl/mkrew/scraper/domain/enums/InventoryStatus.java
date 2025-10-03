package pl.mkrew.scraper.domain.enums;

public enum InventoryStatus {
    LOW(1, "Niski"),
    MEDIUM(2, "Średni"),
    SATISFACTORY(3, "Zadowalający"),
    HIGH(4, "Wysoki");

    private final int level;
    private final String displayName;

    InventoryStatus(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static InventoryStatus fromLevel(int level) {
        for (InventoryStatus status : values()) {
            if (status.level == level) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown inventory status level: " + level);
    }
}
