package onetoone.wellness;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeEnrollmentRepository extends JpaRepository<ChallengeEnrollment, Long> {
    List<ChallengeEnrollment> findByUserOrderByEnrolledAtDesc(User user);
    List<ChallengeEnrollment> findByUser_IdOrderByEnrolledAtDesc(Integer userId);
    Optional<ChallengeEnrollment> findByUser_IdAndChallenge_Id(Integer userId, Long challengeId);
}

