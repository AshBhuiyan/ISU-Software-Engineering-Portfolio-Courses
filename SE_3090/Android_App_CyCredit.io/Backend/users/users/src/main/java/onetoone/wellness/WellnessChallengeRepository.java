package onetoone.wellness;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WellnessChallengeRepository extends JpaRepository<WellnessChallenge, Long> {
    List<WellnessChallenge> findByIsActiveTrue();
    Optional<WellnessChallenge> findFirstByIsActiveTrue();
}

