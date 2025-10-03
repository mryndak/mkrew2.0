package pl.mkrew.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mkrew.backend.domain.entity.*;
import pl.mkrew.backend.domain.enums.ForecastStatus;
import pl.mkrew.backend.domain.enums.InventoryStatus;
import pl.mkrew.backend.dto.*;
import pl.mkrew.backend.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForecastService {

    private static final Logger logger = LoggerFactory.getLogger(ForecastService.class);

    private final ForecastRequestRepository forecastRequestRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final ForecastModelRepository forecastModelRepository;
    private final RCKiKRepository rckikRepository;
    private final BloodInventoryRecordRepository bloodInventoryRecordRepository;
    private final MLServiceClient mlServiceClient;

    public ForecastService(ForecastRequestRepository forecastRequestRepository,
                           ForecastResultRepository forecastResultRepository,
                           ForecastModelRepository forecastModelRepository,
                           RCKiKRepository rckikRepository,
                           BloodInventoryRecordRepository bloodInventoryRecordRepository,
                           MLServiceClient mlServiceClient) {
        this.forecastRequestRepository = forecastRequestRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.forecastModelRepository = forecastModelRepository;
        this.rckikRepository = rckikRepository;
        this.bloodInventoryRecordRepository = bloodInventoryRecordRepository;
        this.mlServiceClient = mlServiceClient;
    }

    @Transactional
    public ForecastResponseDto createForecastRequest(ForecastRequestDto requestDto, User user) {
        logger.info("Creating forecast request for RCKiK ID: {}, Model: {}",
            requestDto.getRckikId(), requestDto.getModelType());

        // Validate RCKiK exists
        RCKiK rckik = rckikRepository.findById(requestDto.getRckikId())
            .orElseThrow(() -> new IllegalArgumentException("RCKiK not found with ID: " + requestDto.getRckikId()));

        // Find active model
        ForecastModel forecastModel = forecastModelRepository
            .findByModelTypeAndIsActiveTrue(requestDto.getModelType())
            .orElseThrow(() -> new IllegalArgumentException("No active model found for type: " + requestDto.getModelType()));

        // Create forecast request entity
        ForecastRequest forecastRequest = new ForecastRequest();
        forecastRequest.setRckik(rckik);
        forecastRequest.setForecastModel(forecastModel);
        forecastRequest.setBloodType(requestDto.getBloodType());
        forecastRequest.setForecastHorizonDays(requestDto.getForecastHorizonDays());
        forecastRequest.setDataStartDate(requestDto.getDataStartDate());
        forecastRequest.setDataEndDate(requestDto.getDataEndDate());
        forecastRequest.setStatus(ForecastStatus.PENDING);
        forecastRequest.setRequestedBy(user);

        forecastRequest = forecastRequestRepository.save(forecastRequest);
        logger.info("Forecast request created with ID: {}", forecastRequest.getId());

        // Process forecast asynchronously (for now synchronously)
        processForecast(forecastRequest);

        return mapToResponseDto(forecastRequest);
    }

    @Transactional
    public void processForecast(ForecastRequest forecastRequest) {
        try {
            logger.info("Processing forecast request ID: {}", forecastRequest.getId());

            forecastRequest.setStatus(ForecastStatus.PROCESSING);
            forecastRequestRepository.save(forecastRequest);

            // Fetch historical data from database
            List<BloodInventoryRecord> historicalRecords = bloodInventoryRecordRepository
                .findByRckikIdAndBloodTypeAndRecordedAtBetween(
                    forecastRequest.getRckik().getId(),
                    forecastRequest.getBloodType(),
                    forecastRequest.getDataStartDate(),
                    forecastRequest.getDataEndDate()
                );

            logger.info("Retrieved {} historical records", historicalRecords.size());

            // Prepare ML request
            MLForecastRequest mlRequest = new MLForecastRequest();
            mlRequest.setModelType(forecastRequest.getForecastModel().getModelType());
            mlRequest.setBloodType(forecastRequest.getBloodType());
            mlRequest.setForecastHorizonDays(forecastRequest.getForecastHorizonDays());

            List<MLForecastRequest.MLDataPoint> dataPoints = historicalRecords.stream()
                .map(record -> {
                    MLForecastRequest.MLDataPoint dp = new MLForecastRequest.MLDataPoint();
                    dp.setTimestamp(record.getRecordedAt());
                    dp.setStatus(record.getInventoryStatus().name());
                    dp.setQuantityLevel(record.getQuantityLevel());
                    return dp;
                })
                .collect(Collectors.toList());

            mlRequest.setHistoricalData(dataPoints);

            // Call ML service
            MLForecastResponse mlResponse = mlServiceClient.requestForecast(mlRequest);

            if (mlResponse.isSuccess() && mlResponse.getPredictions() != null) {
                // Save forecast results
                List<ForecastResult> results = mlResponse.getPredictions().stream()
                    .map(prediction -> {
                        ForecastResult result = new ForecastResult();
                        result.setForecastRequest(forecastRequest);
                        result.setForecastDate(prediction.getForecastDate());
                        result.setPredictedStatus(InventoryStatus.valueOf(prediction.getPredictedStatus()));
                        result.setPredictedQuantity(prediction.getPredictedQuantity());
                        result.setConfidenceLower(prediction.getConfidenceLower());
                        result.setConfidenceUpper(prediction.getConfidenceUpper());
                        result.setConfidenceLevel(prediction.getConfideLevel());
                        return result;
                    })
                    .collect(Collectors.toList());

                forecastResultRepository.saveAll(results);

                forecastRequest.setStatus(ForecastStatus.COMPLETED);
                forecastRequest.setCompletedAt(LocalDateTime.now());
                logger.info("Forecast completed successfully with {} results", results.size());
            } else {
                forecastRequest.setStatus(ForecastStatus.FAILED);
                forecastRequest.setErrorMessage(mlResponse.getErrorMessage());
                logger.error("Forecast failed: {}", mlResponse.getErrorMessage());
            }

            forecastRequestRepository.save(forecastRequest);

        } catch (Exception e) {
            logger.error("Error processing forecast request", e);
            forecastRequest.setStatus(ForecastStatus.FAILED);
            forecastRequest.setErrorMessage("Error processing forecast: " + e.getMessage());
            forecastRequestRepository.save(forecastRequest);
        }
    }

    public ForecastResponseDto getForecastById(Long id) {
        ForecastRequest forecastRequest = forecastRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Forecast request not found with ID: " + id));
        return mapToResponseDto(forecastRequest);
    }

    public List<ForecastResponseDto> getAllForecasts() {
        return forecastRequestRepository.findAll().stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
    }

    public List<ForecastResponseDto> getForecastsByRckik(Long rckikId) {
        return forecastRequestRepository.findByRckikIdOrderByCreatedAtDesc(rckikId).stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
    }

    private ForecastResponseDto mapToResponseDto(ForecastRequest forecastRequest) {
        ForecastResponseDto dto = new ForecastResponseDto();
        dto.setRequestId(forecastRequest.getId());
        dto.setRckikId(forecastRequest.getRckik().getId());
        dto.setRckikName(forecastRequest.getRckik().getName());
        dto.setModelType(forecastRequest.getForecastModel().getModelType());
        dto.setBloodType(forecastRequest.getBloodType());
        dto.setForecastHorizonDays(forecastRequest.getForecastHorizonDays());
        dto.setStatus(forecastRequest.getStatus());
        dto.setCreatedAt(forecastRequest.getCreatedAt());
        dto.setCompletedAt(forecastRequest.getCompletedAt());
        dto.setErrorMessage(forecastRequest.getErrorMessage());

        // Load results if completed
        if (forecastRequest.getStatus() == ForecastStatus.COMPLETED) {
            List<ForecastResult> results = forecastResultRepository
                .findByForecastRequestIdOrderByForecastDateAsc(forecastRequest.getId());

            List<ForecastResultDto> resultDtos = results.stream()
                .map(result -> {
                    ForecastResultDto resultDto = new ForecastResultDto();
                    resultDto.setForecastDate(result.getForecastDate());
                    resultDto.setPredictedStatus(result.getPredictedStatus());
                    resultDto.setPredictedQuantity(result.getPredictedQuantity());
                    resultDto.setConfidenceLower(result.getConfidenceLower());
                    resultDto.setConfidenceUpper(result.getConfidenceUpper());
                    resultDto.setConfidenceLevel(result.getConfidenceLevel());
                    return resultDto;
                })
                .collect(Collectors.toList());

            dto.setResults(resultDtos);
        }

        return dto;
    }
}
