package pl.mkrew.backend.dto;

import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.domain.enums.ForecastModelType;

import java.time.LocalDateTime;
import java.util.List;

public class MLForecastRequest {

    private ForecastModelType modelType;
    private BloodType bloodType;
    private Integer forecastHorizonDays;
    private List<MLDataPoint> historicalData;

    public MLForecastRequest() {
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

    public List<MLDataPoint> getHistoricalData() {
        return historicalData;
    }

    public void setHistoricalData(List<MLDataPoint> historicalData) {
        this.historicalData = historicalData;
    }

    public static class MLDataPoint {
        private LocalDateTime timestamp;
        private String status;
        private Integer quantityLevel;

        public MLDataPoint() {
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getQuantityLevel() {
            return quantityLevel;
        }

        public void setQuantityLevel(Integer quantityLevel) {
            this.quantityLevel = quantityLevel;
        }
    }
}
