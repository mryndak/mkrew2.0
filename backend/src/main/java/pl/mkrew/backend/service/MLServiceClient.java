package pl.mkrew.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.mkrew.backend.dto.MLForecastRequest;
import pl.mkrew.backend.dto.MLForecastResponse;

@Service
public class MLServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MLServiceClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MLServiceClient(RestTemplate restTemplate, @Value("${ml.service.url}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public MLForecastResponse requestForecast(MLForecastRequest request) {
        try {
            logger.info("Sending forecast request to ML service: {}", mlServiceUrl);

            String endpoint = mlServiceUrl + "/api/forecast";
            ResponseEntity<MLForecastResponse> response = restTemplate.postForEntity(
                endpoint,
                request,
                MLForecastResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Forecast request successful");
                return response.getBody();
            } else {
                logger.error("ML service returned non-OK status: {}", response.getStatusCode());
                MLForecastResponse errorResponse = new MLForecastResponse();
                errorResponse.setSuccess(false);
                errorResponse.setErrorMessage("ML service returned status: " + response.getStatusCode());
                return errorResponse;
            }
        } catch (Exception e) {
            logger.error("Error communicating with ML service", e);
            MLForecastResponse errorResponse = new MLForecastResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Error communicating with ML service: " + e.getMessage());
            return errorResponse;
        }
    }
}
