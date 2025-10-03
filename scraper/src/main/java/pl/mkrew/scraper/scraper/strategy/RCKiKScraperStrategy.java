package pl.mkrew.scraper.scraper.strategy;

import pl.mkrew.scraper.scraper.dto.BloodInventoryData;

import java.util.List;

/**
 * Strategy interface for scraping blood inventory data from different RCKiK websites.
 * Each RCKiK may have different website structure, so we use Strategy pattern
 * to handle each one differently.
 */
public interface RCKiKScraperStrategy {

    /**
     * Scrapes blood inventory data from RCKiK website
     * @return List of blood inventory data for all blood types
     * @throws Exception if scraping fails
     */
    List<BloodInventoryData> scrape() throws Exception;

    /**
     * Returns the RCKiK code this strategy handles
     * @return RCKiK code (e.g., "RZESZOW", "KRAKOW")
     */
    String getRCKiKCode();

    /**
     * Returns the website URL
     * @return URL of the RCKiK website
     */
    String getWebsiteUrl();
}
