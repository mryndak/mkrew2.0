package pl.mkrew.scraper.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.mkrew.scraper.service.ScraperService;

@Component
public class ScrapingJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ScrapingJob.class);

    private final ScraperService scraperService;

    public ScrapingJob(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting scheduled scraping job");
        try {
            scraperService.scrapeAll();
            log.info("Scheduled scraping job completed successfully");
        } catch (Exception e) {
            log.error("Scheduled scraping job failed: {}", e.getMessage(), e);
            throw new JobExecutionException("Scraping job failed", e);
        }
    }
}
