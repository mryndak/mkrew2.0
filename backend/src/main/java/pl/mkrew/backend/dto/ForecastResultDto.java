package pl.mkrew.backend.dto;

import pl.mkrew.backend.domain.enums.InventoryStatus;

import java.time.LocalDateTime;

public class ForecastResultDto {

    private LocalDateTime forecastDate;
    private InventoryStatus predictedStatus;
    private Double predictedQuantity;
    private Double confidenceLower;
    private Double confidenceUpper;
    private Double confidenceLevel;

    public ForecastResultDto() {
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
}
