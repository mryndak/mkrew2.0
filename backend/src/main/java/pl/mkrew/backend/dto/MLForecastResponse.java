package pl.mkrew.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MLForecastResponse {

    private boolean success;
    private String errorMessage;
    private List<MLPrediction> predictions;

    public MLForecastResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<MLPrediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<MLPrediction> predictions) {
        this.predictions = predictions;
    }

    public static class MLPrediction {
        private LocalDateTime forecastDate;
        private String predictedStatus;
        private Double predictedQuantity;
        private Double confidenceLower;
        private Double confidenceUpper;
        private Double confidenceLevel;

        public MLPrediction() {
        }

        public LocalDateTime getForecastDate() {
            return forecastDate;
        }

        public void setForecastDate(LocalDateTime forecastDate) {
            this.forecastDate = forecastDate;
        }

        public String getPredictedStatus() {
            return predictedStatus;
        }

        public void setPredictedStatus(String predictedStatus) {
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
}
