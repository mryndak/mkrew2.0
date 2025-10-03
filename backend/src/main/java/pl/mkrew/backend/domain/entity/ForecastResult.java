package pl.mkrew.backend.domain.entity;

import jakarta.persistence.*;
import pl.mkrew.backend.domain.enums.InventoryStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "forecast_result", indexes = {
    @Index(name = "idx_forecast_result_request_id", columnList = "forecast_request_id"),
    @Index(name = "idx_forecast_result_forecast_date", columnList = "forecast_date"),
    @Index(name = "idx_forecast_result_composite", columnList = "forecast_request_id, forecast_date")
})
public class ForecastResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_request_id", nullable = false)
    private ForecastRequest forecastRequest;

    @Column(name = "forecast_date", nullable = false)
    private LocalDateTime forecastDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_status", length = 20)
    private InventoryStatus predictedStatus;

    @Column(name = "predicted_quantity")
    private Double predictedQuantity;

    @Column(name = "confidence_lower")
    private Double confidenceLower;

    @Column(name = "confidence_upper")
    private Double confidenceUpper;

    @Column(name = "confidence_level")
    private Double confidenceLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ForecastResult() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ForecastRequest getForecastRequest() {
        return forecastRequest;
    }

    public void setForecastRequest(ForecastRequest forecastRequest) {
        this.forecastRequest = forecastRequest;
    }

    public LocalDateTime getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDateTime forecastDate) {
        this.forecastDate = forecastDate;
    }

    public InventoryStatus getPredictedStatus() {
        return predictedStatus;
    }

    public void setPredictedStatus(InventoryStatus predictedStatus) {
        this.predictedStatus = predictedStatus;
    }

    public Double getPredictedQuantity() {
        return predictedQuantity;
    }

    public void setPredictedQuantity(Double predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
    }

    public Double getConfidenceLower() {
        return confidenceLower;
    }

    public void setConfidenceLower(Double confidenceLower) {
        this.confidenceLower = confidenceLower;
    }

    public Double getConfidenceUpper() {
        return confidenceUpper;
    }

    public void setConfidenceUpper(Double confidenceUpper) {
        this.confidenceUpper = confidenceUpper;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
