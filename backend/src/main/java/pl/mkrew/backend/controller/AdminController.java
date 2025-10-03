package pl.mkrew.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.mkrew.backend.service.ScraperClientService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ScraperClientService scraperClientService;

    public AdminController(ScraperClientService scraperClientService) {
        this.scraperClientService = scraperClientService;
    }

    @PostMapping("/scraper/trigger-all")
    public ResponseEntity<Map<String, Object>> triggerScrapingAll() {
        try {
            Map<String, Object> result = scraperClientService.triggerScrapingAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/scraper/trigger/{rckikCode}")
    public ResponseEntity<Map<String, Object>> triggerScrapingByRCKiK(@PathVariable String rckikCode) {
        try {
            Map<String, Object> result = scraperClientService.triggerScrapingByRCKiK(rckikCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
