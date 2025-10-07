package pl.mkrew.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ScraperClientService {

    private static final Logger log = LoggerFactory.getLogger(ScraperClientService.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${scraper.service.url}")
    private String scraperServiceUrl;

    @Value("${scraper.api.key}")
    private String scraperApiKey;

    private final RestTemplate restTemplate;

    public ScraperClientService() {
        this.restTemplate = new RestTemplate();
    }

    private HttpEntity<Void> createAuthenticatedRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, scraperApiKey);
        return new HttpEntity<>(headers);
    }

    public Map<String, Object> triggerScrapingAll() {
        String url = scraperServiceUrl + "/api/scraper/trigger-all";
        log.info("Triggering scraping for all RCKiK at: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, createAuthenticatedRequest(), Map.class);
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
            ResponseEntity<Map> response = restTemplate.postForEntity(url, createAuthenticatedRequest(), Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to trigger scraping for {}: {}", rckikCode, e.getMessage());
            throw new RuntimeException("Failed to trigger scraping: " + e.getMessage());
        }
    }
}
