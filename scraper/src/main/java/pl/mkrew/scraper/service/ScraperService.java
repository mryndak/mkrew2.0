package pl.mkrew.scraper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mkrew.scraper.domain.entity.BloodInventoryRecord;
import pl.mkrew.scraper.domain.entity.RCKiK;
import pl.mkrew.scraper.domain.entity.ScrapingLog;
import pl.mkrew.scraper.domain.enums.ScrapingStatus;
import pl.mkrew.scraper.repository.BloodInventoryRecordRepository;
import pl.mkrew.scraper.repository.RCKiKRepository;
import pl.mkrew.scraper.repository.ScrapingLogRepository;
import pl.mkrew.scraper.scraper.dto.BloodInventoryData;
import pl.mkrew.scraper.scraper.strategy.RCKiKScraperStrategy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final Map<String, RCKiKScraperStrategy> strategies;
    private final RCKiKRepository rckikRepository;
    private final BloodInventoryRecordRepository bloodInventoryRecordRepository;
    private final ScrapingLogRepository scrapingLogRepository;

    public ScraperService(List<RCKiKScraperStrategy> strategyList,
                          RCKiKRepository rckikRepository,
                          BloodInventoryRecordRepository bloodInventoryRecordRepository,
                          ScrapingLogRepository scrapingLogRepository) {
        this.strategies = new HashMap<>();
        strategyList.forEach(strategy ->
                strategies.put(strategy.getRCKiKCode(), strategy));
        this.rckikRepository = rckikRepository;
        this.bloodInventoryRecordRepository = bloodInventoryRecordRepository;
        this.scrapingLogRepository = scrapingLogRepository;
        log.info("Initialized ScraperService with {} strategies: {}",
                strategies.size(), strategies.keySet());
    }

    /**
     * Scrapes all enabled RCKiK centers
     */
    @Transactional
    public void scrapeAll() {
        log.info("Starting scraping for all enabled RCKiK centers");
        List<RCKiK> enabledCenters = rckikRepository.findAllEnabledForScraping();

        for (RCKiK rckik : enabledCenters) {
            scrapeRCKiK(rckik.getCode());
        }
        log.info("Completed scraping for all enabled RCKiK centers");
    }

    /**
     * Scrapes specific RCKiK by code
     */
    @Transactional
    public ScrapingLog scrapeRCKiK(String rckikCode) {
        log.info("Starting scraping for RCKiK: {}", rckikCode);

        Optional<RCKiK> rckikOpt = rckikRepository.findByCode(rckikCode);
        if (rckikOpt.isEmpty()) {
            log.error("RCKiK not found: {}", rckikCode);
            throw new IllegalArgumentException("RCKiK not found: " + rckikCode);
        }

        RCKiK rckik = rckikOpt.get();
        RCKiKScraperStrategy strategy = strategies.get(rckikCode);

        if (strategy == null) {
            log.error("No scraper strategy found for RCKiK: {}", rckikCode);
            return createFailedLog(rckik, "No scraper strategy found for " + rckikCode, 0);
        }

        long startTime = System.currentTimeMillis();
        ScrapingLog scrapingLog = new ScrapingLog();
        scrapingLog.setRckik(rckik);
        scrapingLog.setExecutedAt(LocalDateTime.now());

        try {
            List<BloodInventoryData> dataList = strategy.scrape();

            int savedCount = 0;
            for (BloodInventoryData data : dataList) {
                try {
                    saveBloodInventoryRecord(rckik, data);
                    savedCount++;
                } catch (Exception e) {
                    log.error("Failed to save blood inventory record for {} - {}: {}",
                            rckikCode, data.getBloodType(), e.getMessage());
                }
            }

            scrapingLog.setStatus(savedCount == dataList.size() ?
                    ScrapingStatus.SUCCESS : ScrapingStatus.PARTIAL);
            scrapingLog.setRecordsScraped(savedCount);
            scrapingLog.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("Successfully scraped {} records for RCKiK: {}", savedCount, rckikCode);

        } catch (Exception e) {
            log.error("Failed to scrape RCKiK {}: {}", rckikCode, e.getMessage(), e);
            scrapingLog.setStatus(ScrapingStatus.FAILED);
            scrapingLog.setErrorMessage(e.getMessage());
            scrapingLog.setRecordsScraped(0);
            scrapingLog.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return scrapingLogRepository.save(scrapingLog);
    }

    private void saveBloodInventoryRecord(RCKiK rckik, BloodInventoryData data) {
        BloodInventoryRecord record = new BloodInventoryRecord();
        record.setRckik(rckik);
        record.setBloodType(data.getBloodType());
        record.setRecordedAt(data.getRecordedAt());
        record.setInventoryStatus(data.getInventoryStatus());
        record.setQuantityLevel(data.getQuantityLevel());
        record.setNotes(data.getNotes());
        record.setSourceUrl(data.getSourceUrl());
        record.setScrapedAt(LocalDateTime.now());

        bloodInventoryRecordRepository.save(record);
    }

    private ScrapingLog createFailedLog(RCKiK rckik, String errorMessage, long duration) {
        ScrapingLog log = new ScrapingLog();
        log.setRckik(rckik);
        log.setExecutedAt(LocalDateTime.now());
        log.setStatus(ScrapingStatus.FAILED);
        log.setErrorMessage(errorMessage);
        log.setRecordsScraped(0);
        log.setDurationMs(duration);
        return scrapingLogRepository.save(log);
    }
}
