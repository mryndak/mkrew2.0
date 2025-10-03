package pl.mkrew.scraper.domain.entity;

import jakarta.persistence.*;
import pl.mkrew.scraper.domain.enums.ScrapingStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "scraping_log", indexes = {
    @Index(name = "idx_scraping_log_rckik_id", columnList = "rckik_id"),
    @Index(name = "idx_scraping_log_executed_at", columnList = "executed_at"),
    @Index(name = "idx_scraping_log_status", columnList = "status")
})
public class ScrapingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rckik_id", nullable = false)
    private RCKiK rckik;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScrapingStatus status;

    @Column(name = "records_scraped")
    private Integer recordsScraped = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    public ScrapingLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RCKiK getRckik() {
        return rckik;
    }

    public void setRckik(RCKiK rckik) {
        this.rckik = rckik;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public ScrapingStatus getStatus() {
        return status;
    }

    public void setStatus(ScrapingStatus status) {
        this.status = status;
    }

    public Integer getRecordsScraped() {
        return recordsScraped;
    }

    public void setRecordsScraped(Integer recordsScraped) {
        this.recordsScraped = recordsScraped;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
