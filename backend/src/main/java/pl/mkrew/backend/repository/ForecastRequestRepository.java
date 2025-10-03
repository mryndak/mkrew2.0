package pl.mkrew.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mkrew.backend.domain.entity.ForecastRequest;
import pl.mkrew.backend.domain.enums.ForecastStatus;

import java.util.List;

@Repository
public interface ForecastRequestRepository extends JpaRepository<ForecastRequest, Long> {

    List<ForecastRequest> findByStatus(ForecastStatus status);

    List<ForecastRequest> findByRckikIdOrderByCreatedAtDesc(Long rckikId);

    List<ForecastRequest> findByRequestedByIdOrderByCreatedAtDesc(Long userId);
}
