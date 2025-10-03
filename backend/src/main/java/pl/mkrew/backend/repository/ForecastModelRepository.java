package pl.mkrew.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mkrew.backend.domain.entity.ForecastModel;
import pl.mkrew.backend.domain.enums.ForecastModelType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {

    List<ForecastModel> findByIsActiveTrue();

    Optional<ForecastModel> findByModelTypeAndIsActiveTrue(ForecastModelType modelType);

    List<ForecastModel> findByModelType(ForecastModelType modelType);
}
