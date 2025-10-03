package pl.mkrew.backend.dto;

import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.domain.enums.InventoryStatus;

import java.time.LocalDateTime;

public class BloodInventoryResponse {
    private String rckikCode;
    private String rckikName;
    private BloodType bloodType;
    private InventoryStatus status;
    private LocalDateTime recordedAt;
    private String notes;

    public BloodInventoryResponse() {
    }

    // Getters and setters
    public String getRckikCode() {
        return rckikCode;
    }

    public void setRckikCode(String rckikCode) {
        this.rckikCode = rckikCode;
    }

    public String getRckikName() {
        return rckikName;
    }

    public void setRckikName(String rckikName) {
        this.rckikName = rckikName;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
