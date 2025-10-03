package pl.mkrew.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.mkrew.backend.domain.entity.RCKiK;

import java.util.Optional;

@Repository
public interface RCKiKRepository extends JpaRepository<RCKiK, Long> {
    Optional<RCKiK> findByCode(String code);
}
