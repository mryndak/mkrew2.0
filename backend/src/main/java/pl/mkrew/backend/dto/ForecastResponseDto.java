package pl.mkrew.backend.dto;

import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.domain.enums.ForecastModelType;
import pl.mkrew.backend.domain.enums.ForecastStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ForecastResponseDto {

    private Long requestId;
    private Long rckikId;
    private String rckikName;
    private ForecastModelType modelType;
    private BloodType bloodType;
    private Integer forecastHorizonDays;
    private ForecastStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private List<ForecastResultDto> results;

    public ForecastResponseDto() {
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getRckikId() {
        return rckikId;
    }

    public void setRckikId(Long rckikId) {
        this.rckikId = rckikId;
    }

    public String getRckikName() {
        return rckikName;
    }

    public void setRckikName(String rckikName) {
        this.rckikName = rckikName;
    }

    public ForecastModelType getModelType() {
        return modelType;
    }

    public void setModelType(ForecastModelType modelType) {
        this.modelType = modelType;
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

    public List<ForecastResultDto> getResults() {
        return results;
    }

    public void setResults(List<ForecastResultDto> results) {
        this.results = results;
    }
}
