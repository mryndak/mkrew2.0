package pl.mkrew.scraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.mkrew.scraper.domain.entity.RCKiK;

import java.util.List;
import java.util.Optional;

@Repository
public interface RCKiKRepository extends JpaRepository<RCKiK, Long> {

    Optional<RCKiK> findByCode(String code);

    @Query("SELECT r FROM RCKiK r WHERE r.scrapingEnabled = true")
    List<RCKiK> findAllEnabledForScraping();
}
