package pl.mkrew.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mkrew.backend.domain.entity.ForecastResult;

import java.util.List;

@Repository
public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {

    List<ForecastResult> findByForecastRequestIdOrderByForecastDateAsc(Long forecastRequestId);

    void deleteByForecastRequestId(Long forecastRequestId);
}
