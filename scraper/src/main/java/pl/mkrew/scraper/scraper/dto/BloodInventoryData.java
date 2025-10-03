package pl.mkrew.scraper.scraper.dto;

import pl.mkrew.scraper.domain.enums.BloodType;
import pl.mkrew.scraper.domain.enums.InventoryStatus;

import java.time.LocalDateTime;

public class BloodInventoryData {
    private BloodType bloodType;
    private InventoryStatus inventoryStatus;
    private Integer quantityLevel;
    private String notes;
    private String sourceUrl;
    private LocalDateTime recordedAt;

    public BloodInventoryData() {
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Integer getQuantityLevel() {
        return quantityLevel;
    }

    public void setQuantityLevel(Integer quantityLevel) {
        this.quantityLevel = quantityLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
