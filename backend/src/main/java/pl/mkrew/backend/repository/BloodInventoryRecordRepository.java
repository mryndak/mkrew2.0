package pl.mkrew.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.mkrew.backend.domain.entity.BloodInventoryRecord;
import pl.mkrew.backend.domain.entity.RCKiK;
import pl.mkrew.backend.domain.enums.BloodType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodInventoryRecordRepository extends JpaRepository<BloodInventoryRecord, Long> {

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.rckik = :rckik AND b.bloodType = :bloodType " +
           "ORDER BY b.recordedAt DESC LIMIT 1")
    Optional<BloodInventoryRecord> findLatestByRckikAndBloodType(
        @Param("rckik") RCKiK rckik,
        @Param("bloodType") BloodType bloodType
    );

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.rckik = :rckik " +
           "AND b.bloodType = :bloodType AND b.recordedAt >= :startDate " +
           "ORDER BY b.recordedAt DESC")
    List<BloodInventoryRecord> findHistoryByRckikAndBloodType(
        @Param("rckik") RCKiK rckik,
        @Param("bloodType") BloodType bloodType,
        @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.id IN " +
           "(SELECT MAX(b2.id) FROM BloodInventoryRecord b2 GROUP BY b2.rckik, b2.bloodType)")
    List<BloodInventoryRecord> findAllLatestRecords();

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.rckik = :rckik AND b.id IN " +
           "(SELECT MAX(b2.id) FROM BloodInventoryRecord b2 WHERE b2.rckik = :rckik GROUP BY b2.bloodType)")
    List<BloodInventoryRecord> findLatestByRckik(@Param("rckik") RCKiK rckik);

    @Query("SELECT b FROM BloodInventoryRecord b WHERE b.rckik.id = :rckikId " +
           "AND b.bloodType = :bloodType AND b.recordedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY b.recordedAt ASC")
    List<BloodInventoryRecord> findByRckikIdAndBloodTypeAndRecordedAtBetween(
        @Param("rckikId") Long rckikId,
        @Param("bloodType") BloodType bloodType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
