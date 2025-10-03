package pl.mkrew.backend.service;

import org.springframework.stereotype.Service;
import pl.mkrew.backend.domain.entity.BloodInventoryRecord;
import pl.mkrew.backend.domain.entity.RCKiK;
import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.dto.BloodInventoryResponse;
import pl.mkrew.backend.repository.BloodInventoryRecordRepository;
import pl.mkrew.backend.repository.RCKiKRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BloodInventoryService {

    private final BloodInventoryRecordRepository bloodInventoryRecordRepository;
    private final RCKiKRepository rckikRepository;

    public BloodInventoryService(BloodInventoryRecordRepository bloodInventoryRecordRepository,
                                  RCKiKRepository rckikRepository) {
        this.bloodInventoryRecordRepository = bloodInventoryRecordRepository;
        this.rckikRepository = rckikRepository;
    }

    public List<BloodInventoryResponse> getAllCurrentInventory() {
        List<BloodInventoryRecord> records = bloodInventoryRecordRepository.findAllLatestRecords();
        return records.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BloodInventoryResponse> getCurrentInventoryByRCKiK(String rckikCode) {
        RCKiK rckik = rckikRepository.findByCode(rckikCode)
                .orElseThrow(() -> new IllegalArgumentException("RCKiK not found: " + rckikCode));

        List<BloodInventoryRecord> records = bloodInventoryRecordRepository.findLatestByRckik(rckik);
        return records.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BloodInventoryResponse> getHistory(String rckikCode, BloodType bloodType, int periodDays) {
        RCKiK rckik = rckikRepository.findByCode(rckikCode)
                .orElseThrow(() -> new IllegalArgumentException("RCKiK not found: " + rckikCode));

        LocalDateTime startDate = LocalDateTime.now().minusDays(periodDays);
        List<BloodInventoryRecord> records = bloodInventoryRecordRepository
                .findHistoryByRckikAndBloodType(rckik, bloodType, startDate);

        return records.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BloodInventoryResponse mapToResponse(BloodInventoryRecord record) {
        BloodInventoryResponse response = new BloodInventoryResponse();
        response.setRckikCode(record.getRckik().getCode());
        response.setRckikName(record.getRckik().getName());
        response.setBloodType(record.getBloodType());
        response.setStatus(record.getInventoryStatus());
        response.setRecordedAt(record.getRecordedAt());
        response.setNotes(record.getNotes());
        return response;
    }
}
