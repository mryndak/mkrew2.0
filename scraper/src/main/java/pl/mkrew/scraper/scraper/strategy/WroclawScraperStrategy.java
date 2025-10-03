package pl.mkrew.scraper.scraper.strategy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.mkrew.scraper.domain.enums.BloodType;
import pl.mkrew.scraper.domain.enums.InventoryStatus;
import pl.mkrew.scraper.scraper.dto.BloodInventoryData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class WroclawScraperStrategy implements RCKiKScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(WroclawScraperStrategy.class);
    private static final String RCKIK_CODE = "WROCLAW";
    private static final String WEBSITE_URL = "https://www.rckik.wroclaw.pl";

    @Override
    public List<BloodInventoryData> scrape() throws Exception {
        log.info("Scraping blood inventory data from RCKiK {}", RCKIK_CODE);

        List<BloodInventoryData> results = new ArrayList<>();
        Document doc = Jsoup.connect(WEBSITE_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        LocalDateTime now = LocalDateTime.now();

        // Real scraping logic for Wrocław - uses image-based status indicators
        // RhD+ groups
        Elements rhdPlusGroups = doc.select("div.blood-inventory-rhd-plus div[class*=blood-group]");
        parseBloodGroups(rhdPlusGroups, "+", now, results);

        // RhD- groups
        Elements rhdMinusGroups = doc.select("div.blood-inventory-rhd-minus div[class*=blood-group]");
        parseBloodGroups(rhdMinusGroups, "-", now, results);

        log.info("Scraped {} blood inventory records from {}", results.size(), RCKIK_CODE);
        return results;
    }

    @Override
    public String getRCKiKCode() {
        return RCKIK_CODE;
    }

    @Override
    public String getWebsiteUrl() {
        return WEBSITE_URL;
    }

    private void parseBloodGroups(Elements bloodGroups, String rhdSuffix, LocalDateTime recordedAt, List<BloodInventoryData> results) {
        for (Element group : bloodGroups) {
            try {
                BloodInventoryData data = new BloodInventoryData();

                // Extract blood type from class (e.g., "blood-group-a" -> "A")
                String className = group.attr("class");
                String bloodTypeLetter = extractBloodTypeLetter(className);
                String bloodTypeStr = bloodTypeLetter + " RhD" + rhdSuffix;

                data.setBloodType(parseBloodType(bloodTypeStr));

                // Parse status from image filename (pojemnik_1.png -> wysoki, pojemnik_4.png -> niski)
                Element img = group.selectFirst("img");
                if (img != null) {
                    String imgSrc = img.attr("src");
                    String alt = img.attr("alt");
                    data.setInventoryStatus(parseStatusFromImage(imgSrc, alt));
                }

                data.setSourceUrl(WEBSITE_URL);
                data.setRecordedAt(recordedAt);

                results.add(data);
            } catch (Exception e) {
                log.warn("Failed to parse blood group element: {}", e.getMessage());
            }
        }
    }

    private String extractBloodTypeLetter(String className) {
        if (className.contains("blood-group-a")) return "A";
        if (className.contains("blood-group-b")) return "B";
        if (className.contains("blood-group-ab")) return "AB";
        if (className.contains("blood-group-o")) return "0";
        throw new IllegalArgumentException("Unknown blood group class: " + className);
    }

    private BloodType parseBloodType(String text) {
        String normalized = text.toUpperCase().replaceAll("\\s+", "");
        return switch (normalized) {
            case "0RHD+", "0+", "0RH+" -> BloodType.O_POSITIVE;
            case "0RHD-", "0-", "0RH-" -> BloodType.O_NEGATIVE;
            case "ARHD+", "A+", "ARH+" -> BloodType.A_POSITIVE;
            case "ARHD-", "A-", "ARH-" -> BloodType.A_NEGATIVE;
            case "BRHD+", "B+", "BRH+" -> BloodType.B_POSITIVE;
            case "BRHD-", "B-", "BRH-" -> BloodType.B_NEGATIVE;
            case "ABRHD+", "AB+", "ABRH+" -> BloodType.AB_POSITIVE;
            case "ABRHD-", "AB-", "ABRH-" -> BloodType.AB_NEGATIVE;
            default -> throw new IllegalArgumentException("Unknown blood type: " + text);
        };
    }

    private InventoryStatus parseStatusFromImage(String imgSrc, String alt) {
        String lower = alt.toLowerCase();

        // Parse from alt text first
        if (lower.contains("niski") || lower.contains("low")) {
            return InventoryStatus.LOW;
        } else if (lower.contains("średni") || lower.contains("sredni") || lower.contains("medium")) {
            return InventoryStatus.MEDIUM;
        } else if (lower.contains("wysoki") || lower.contains("high")) {
            return InventoryStatus.HIGH;
        } else if (lower.contains("optymalny") || lower.contains("satisfactory")) {
            return InventoryStatus.SATISFACTORY;
        }

        // Fallback: parse from image filename (pojemnik_1 = high, pojemnik_4 = low)
        if (imgSrc.contains("pojemnik_1")) {
            return InventoryStatus.HIGH;
        } else if (imgSrc.contains("pojemnik_2")) {
            return InventoryStatus.SATISFACTORY;
        } else if (imgSrc.contains("pojemnik_3")) {
            return InventoryStatus.MEDIUM;
        } else if (imgSrc.contains("pojemnik_4")) {
            return InventoryStatus.LOW;
        }

        return InventoryStatus.MEDIUM; // Default
    }
}
