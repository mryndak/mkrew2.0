package pl.mkrew.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.mkrew.backend.dto.MLForecastRequest;
import pl.mkrew.backend.dto.MLForecastResponse;

@Service
public class MLServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MLServiceClient.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;
    private final String mlApiKey;

    public MLServiceClient(RestTemplate restTemplate,
                          @Value("${ml.service.url}") String mlServiceUrl,
                          @Value("${ml.api.key}") String mlApiKey) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
        this.mlApiKey = mlApiKey;
    }

    private <T> HttpEntity<T> createAuthenticatedRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, mlApiKey);
        headers.set("Content-Type", "application/json");
        return new HttpEntity<>(body, headers);
    }

    public MLForecastResponse requestForecast(MLForecastRequest request) {
        try {
            logger.info("Sending forecast request to ML service: {}", mlServiceUrl);

            String endpoint = mlServiceUrl + "/api/forecast";
            ResponseEntity<MLForecastResponse> response = restTemplate.postForEntity(
                endpoint,
                createAuthenticatedRequest(request),
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
