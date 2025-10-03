package pl.mkrew.backend.dto;

import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.domain.enums.ForecastModelType;

import java.time.LocalDateTime;

public class ForecastRequestDto {

    private Long rckikId;
    private ForecastModelType modelType;
    private BloodType bloodType;
    private Integer forecastHorizonDays;
    private LocalDateTime dataStartDate;
    private LocalDateTime dataEndDate;

    public ForecastRequestDto() {
    }

    public Long getRckikId() {
        return rckikId;
    }

    public void setRckikId(Long rckikId) {
        this.rckikId = rckikId;
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
}
