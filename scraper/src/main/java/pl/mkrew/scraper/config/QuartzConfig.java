package pl.mkrew.scraper.config;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.mkrew.scraper.scheduler.ScrapingJob;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail scrapingJobDetail() {
        return JobBuilder.newJob(ScrapingJob.class)
                .withIdentity("scrapingJob")
                .withDescription("Scheduled job to scrape blood inventory data from RCKiK websites")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger scrapingJobTrigger() {
        // Cron: 0 0 10 * * ? = Every day at 10:00 AM
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule("0 0 10 * * ?")
                .inTimeZone(java.util.TimeZone.getTimeZone("Europe/Warsaw"));

        return TriggerBuilder.newTrigger()
                .forJob(scrapingJobDetail())
                .withIdentity("scrapingJobTrigger")
                .withDescription("Trigger for daily scraping at 10:00 AM")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
