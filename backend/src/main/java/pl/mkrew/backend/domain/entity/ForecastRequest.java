package pl.mkrew.backend.domain.entity;

import jakarta.persistence.*;
import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.domain.enums.ForecastStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "forecast_request", indexes = {
    @Index(name = "idx_forecast_request_status", columnList = "status"),
    @Index(name = "idx_forecast_request_created_at", columnList = "created_at"),
    @Index(name = "idx_forecast_request_rckik_model", columnList = "rckik_id, forecast_model_id")
})
public class ForecastRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rckik_id", nullable = false)
    private RCKiK rckik;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_model_id", nullable = false)
    private ForecastModel forecastModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 20)
    private BloodType bloodType;

    @Column(name = "forecast_horizon_days", nullable = false)
    private Integer forecastHorizonDays;

    @Column(name = "data_start_date", nullable = false)
    private LocalDateTime dataStartDate;

    @Column(name = "data_end_date", nullable = false)
    private LocalDateTime dataEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ForecastStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    public ForecastRequest() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ForecastStatus.PENDING;
        }
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

    public ForecastModel getForecastModel() {
        return forecastModel;
    }

    public void setForecastModel(ForecastModel forecastModel) {
        this.forecastModel = forecastModel;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public Integer getForecastHorizonDays() {
        return forecastHorizonDays;
    }

    public void setForecastHorizonDays(Integer forecastHorizonDays) {
        this.forecastHorizonDays = forecastHorizonDays;
    }

    public LocalDateTime getDataStartDate() {
        return dataStartDate;
    }

    public void setDataStartDate(LocalDateTime dataStartDate) {
        this.dataStartDate = dataStartDate;
    }

    public LocalDateTime getDataEndDate() {
        return dataEndDate;
    }

    public void setDataEndDate(LocalDateTime dataEndDate) {
        this.dataEndDate = dataEndDate;
    }

    public ForecastStatus getStatus() {
        return status;
    }

    public void setStatus(ForecastStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }
}
