package pl.mkrew.scraper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.mkrew.scraper.domain.entity.ScrapingLog;
import pl.mkrew.scraper.service.ScraperService;

import java.util.Map;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private final ScraperService scraperService;

    public ScraperController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    /**
     * Trigger scraping for all enabled RCKiK centers
     * POST /api/scraper/trigger-all
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<Map<String, String>> triggerAll() {
        try {
            scraperService.scrapeAll();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Scraping triggered for all enabled RCKiK centers"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to trigger scraping: " + e.getMessage()
            ));
        }
    }

    /**
     * Trigger scraping for specific RCKiK by code
     * POST /api/scraper/trigger/{rckikCode}
     */
    @PostMapping("/trigger/{rckikCode}")
    public ResponseEntity<?> triggerByRCKiK(@PathVariable String rckikCode) {
        try {
            ScrapingLog log = scraperService.scrapeRCKiK(rckikCode.toUpperCase());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Scraping completed for " + rckikCode,
                    "scrapingLog", Map.of(
                            "status", log.getStatus(),
                            "recordsScraped", log.getRecordsScraped(),
                            "durationMs", log.getDurationMs()
                    )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to trigger scraping: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/scraper/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "up",
                "service", "scraper"
        ));
    }
}
