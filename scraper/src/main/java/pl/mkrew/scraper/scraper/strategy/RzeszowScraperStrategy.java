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
public class RzeszowScraperStrategy implements RCKiKScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(RzeszowScraperStrategy.class);
    private static final String RCKIK_CODE = "RZESZOW";
    private static final String WEBSITE_URL = "https://www.rckk.rzeszow.pl";

    @Override
    public List<BloodInventoryData> scrape() throws Exception {
        log.info("Scraping blood inventory data from RCKiK {}", RCKIK_CODE);

        List<BloodInventoryData> results = new ArrayList<>();
        Document doc = Jsoup.connect(WEBSITE_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        LocalDateTime now = LocalDateTime.now();

        // Real scraping logic based on actual Rzeszów website structure
        // Structure: <div class="blood-inventory"><h3>Stan krwi na dzień XX-XX-XXXX</h3><ul><li>...</li></ul></div>
        Element bloodInventoryDiv = doc.selectFirst("div.blood-inventory");

        if (bloodInventoryDiv == null) {
            log.warn("Blood inventory section not found on page");
            return results;
        }

        Elements bloodTypeItems = bloodInventoryDiv.select("ul li");

        for (Element item : bloodTypeItems) {
            try {
                BloodInventoryData data = new BloodInventoryData();

                // Get blood type text (e.g., "0 RhD+", "A RhD-")
                String bloodTypeText = item.text().trim();

                // Extract blood type (remove status indicators if any)
                String cleanedBloodType = bloodTypeText.split("\\s+")[0] + " " +
                                         (bloodTypeText.contains("RhD-") ? "RhD-" : "RhD+");

                data.setBloodType(parseBloodType(cleanedBloodType));

                // Parse status from CSS class or icon/image in the element
                String itemClass = item.attr("class");
                data.setInventoryStatus(parseInventoryStatusFromClass(itemClass));

                data.setSourceUrl(WEBSITE_URL);
                data.setRecordedAt(now);

                results.add(data);
            } catch (Exception e) {
                log.warn("Failed to parse blood type element '{}': {}", item.text(), e.getMessage());
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

    private InventoryStatus parseInventoryStatusFromClass(String cssClass) {
        String lower = cssClass.toLowerCase();
        if (lower.contains("niski") || lower.contains("low") || lower.contains("critical")) {
            return InventoryStatus.LOW;
        } else if (lower.contains("sredni") || lower.contains("średni") || lower.contains("medium")) {
            return InventoryStatus.MEDIUM;
        } else if (lower.contains("wysoki") || lower.contains("high")) {
            return InventoryStatus.HIGH;
        } else if (lower.contains("optymalny") || lower.contains("satisfactory") || lower.contains("optimal")) {
            return InventoryStatus.SATISFACTORY;
        } else {
            return InventoryStatus.MEDIUM; // Default fallback
        }
    }
}
