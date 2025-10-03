package pl.mkrew.scraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.mkrew.scraper.domain.entity.RCKiK;
import pl.mkrew.scraper.domain.entity.ScrapingLog;
import pl.mkrew.scraper.domain.enums.ScrapingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapingLogRepository extends JpaRepository<ScrapingLog, Long> {

    List<ScrapingLog> findByRckikOrderByExecutedAtDesc(RCKiK rckik);

    List<ScrapingLog> findByStatusOrderByExecutedAtDesc(ScrapingStatus status);

    @Query("SELECT s FROM ScrapingLog s WHERE s.rckik = :rckik ORDER BY s.executedAt DESC LIMIT 1")
    Optional<ScrapingLog> findLatestByRckik(@Param("rckik") RCKiK rckik);

    @Query("SELECT s FROM ScrapingLog s WHERE s.executedAt >= :startDate ORDER BY s.executedAt DESC")
    List<ScrapingLog> findByExecutedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT s FROM ScrapingLog s WHERE s.rckik = :rckik AND s.executedAt >= :startDate " +
           "ORDER BY s.executedAt DESC")
    List<ScrapingLog> findByRckikAndExecutedAtAfter(
        @Param("rckik") RCKiK rckik,
        @Param("startDate") LocalDateTime startDate
    );
}
