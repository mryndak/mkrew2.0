package pl.mkrew.scraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.mkrew.scraper.domain.entity.BloodInventoryRecord;
import pl.mkrew.scraper.domain.entity.RCKiK;
import pl.mkrew.scraper.domain.enums.BloodType;
import pl.mkrew.scraper.domain.enums.InventoryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodInventoryRecordRepository extends JpaRepository<BloodInventoryRecord, Long> {

    List<BloodInventoryRecord> findByRckikAndBloodTypeOrderByRecordedAtDesc(RCKiK rckik, BloodType bloodType);

    List<BloodInventoryRecord> findByRckikOrderByRecordedAtDesc(RCKiK rckik);

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.rckik = :rckik AND b.bloodType = :bloodType " +
           "AND b.recordedAt >= :startDate ORDER BY b.recordedAt DESC")
    List<BloodInventoryRecord> findByRckikAndBloodTypeAndRecordedAtAfter(
        @Param("rckik") RCKiK rckik,
        @Param("bloodType") BloodType bloodType,
        @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.rckik = :rckik AND b.bloodType = :bloodType " +
           "ORDER BY b.recordedAt DESC LIMIT 1")
    Optional<BloodInventoryRecord> findLatestByRckikAndBloodType(
        @Param("rckik") RCKiK rckik,
        @Param("bloodType") BloodType bloodType
    );

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.inventoryStatus = :status " +
           "AND b.recordedAt >= :startDate ORDER BY b.recordedAt DESC")
    List<BloodInventoryRecord> findByInventoryStatusAndRecordedAtAfter(
        @Param("status") InventoryStatus status,
        @Param("startDate") LocalDateTime startDate
    );
}
