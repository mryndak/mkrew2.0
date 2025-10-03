package pl.mkrew.backend.domain.entity;

import jakarta.persistence.*;
import pl.mkrew.backend.domain.enums.BloodType;
import pl.mkrew.backend.domain.enums.InventoryStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "blood_inventory_record", indexes = {
    @Index(name = "idx_blood_inventory_record_rckik_id", columnList = "rckik_id"),
    @Index(name = "idx_blood_inventory_record_blood_type", columnList = "blood_type"),
    @Index(name = "idx_blood_inventory_record_recorded_at", columnList = "recorded_at"),
    @Index(name = "idx_blood_inventory_record_composite", columnList = "rckik_id, blood_type, recorded_at")
})
public class BloodInventoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rckik_id", nullable = false)
    private RCKiK rckik;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 20)
    private BloodType bloodType;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 20)
    private InventoryStatus inventoryStatus;

    @Column(name = "quantity_level")
    private Integer quantityLevel;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;

    public BloodInventoryRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RCKiK getRckik() {
        return rckik;
    }

    public void setRckik(RCKiK rckik) {
        this.rckik = rckik;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
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

    public LocalDateTime getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(LocalDateTime scrapedAt) {
        this.scrapedAt = scrapedAt;
    }
}
