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
public class KrakowScraperStrategy implements RCKiKScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(KrakowScraperStrategy.class);
    private static final String RCKIK_CODE = "KRAKOW";
    private static final String WEBSITE_URL = "https://rckik.krakow.pl";

    @Override
    public List<BloodInventoryData> scrape() throws Exception {
        log.info("Scraping blood inventory data from RCKiK {}", RCKIK_CODE);

        List<BloodInventoryData> results = new ArrayList<>();
        Document doc = Jsoup.connect(WEBSITE_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        LocalDateTime now = LocalDateTime.now();

        // Example: Kraków might use table structure
        Elements rows = doc.select("table.blood-inventory tr");

        for (Element row : rows) {
            try {
                Elements cells = row.select("td");
                if (cells.size() < 2) continue;

                BloodInventoryData data = new BloodInventoryData();

                String bloodTypeText = cells.get(0).text();
                data.setBloodType(parseBloodType(bloodTypeText));

                String statusText = cells.get(1).text();
                data.setInventoryStatus(parseInventoryStatus(statusText));

                data.setSourceUrl(WEBSITE_URL);
                data.setRecordedAt(now);

                results.add(data);
            } catch (Exception e) {
                log.warn("Failed to parse row: {}", e.getMessage());
            }
        }

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

    private BloodType parseBloodType(String text) {
        return switch (text.toUpperCase().replaceAll("\\s+", "")) {
            case "0+", "0RH+" -> BloodType.O_POSITIVE;
            case "0-", "0RH-" -> BloodType.O_NEGATIVE;
            case "A+", "ARH+" -> BloodType.A_POSITIVE;
            case "A-", "ARH-" -> BloodType.A_NEGATIVE;
            case "B+", "BRH+" -> BloodType.B_POSITIVE;
            case "B-", "BRH-" -> BloodType.B_NEGATIVE;
            case "AB+", "ABRH+" -> BloodType.AB_POSITIVE;
            case "AB-", "ABRH-" -> BloodType.AB_NEGATIVE;
            default -> throw new IllegalArgumentException("Unknown blood type: " + text);
        };
    }

    private InventoryStatus parseInventoryStatus(String text) {
        String normalized = text.toLowerCase();
        if (normalized.contains("niski") || normalized.contains("low")) {
            return InventoryStatus.LOW;
        } else if (normalized.contains("średni") || normalized.contains("medium")) {
            return InventoryStatus.MEDIUM;
        } else if (normalized.contains("wysoki") || normalized.contains("high")) {
            return InventoryStatus.HIGH;
        } else {
            return InventoryStatus.SATISFACTORY;
        }
    }
}
