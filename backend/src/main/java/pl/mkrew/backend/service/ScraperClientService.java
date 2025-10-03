package pl.mkrew.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ScraperClientService {

    private static final Logger log = LoggerFactory.getLogger(ScraperClientService.class);

    @Value("${scraper.service.url}")
    private String scraperServiceUrl;

    private final RestTemplate restTemplate;

    public ScraperClientService() {
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> triggerScrapingAll() {
        String url = scraperServiceUrl + "/api/scraper/trigger-all";
        log.info("Triggering scraping for all RCKiK at: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to trigger scraping: {}", e.getMessage());
            throw new RuntimeException("Failed to trigger scraping: " + e.getMessage());
        }
    }

    public Map<String, Object> triggerScrapingByRCKiK(String rckikCode) {
        String url = scraperServiceUrl + "/api/scraper/trigger/" + rckikCode;
        log.info("Triggering scraping for RCKiK {} at: {}", rckikCode, url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to trigger scraping for {}: {}", rckikCode, e.getMessage());
            throw new RuntimeException("Failed to trigger scraping: " + e.getMessage());
        }
    }
}
