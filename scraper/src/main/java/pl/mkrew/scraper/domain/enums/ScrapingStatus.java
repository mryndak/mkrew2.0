package pl.mkrew.scraper.domain.enums;

public enum ScrapingStatus {
    SUCCESS("Sukces"),
    FAILED("Błąd"),
    PARTIAL("Częściowy sukces");

    private final String displayName;

    ScrapingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
