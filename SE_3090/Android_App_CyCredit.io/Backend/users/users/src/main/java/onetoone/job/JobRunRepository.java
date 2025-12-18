package onetoone.job;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRunRepository extends JpaRepository<JobRun, Long> {
    List<JobRun> findByUserOrderByRunAtDesc(User user);
    List<JobRun> findByUser_IdOrderByRunAtDesc(Integer userId);
    Optional<JobRun> findByRunNonce(String runNonce);
    List<JobRun> findByUser_IdAndPassedTrueAndRunAtAfter(Integer userId, OffsetDateTime after);
}

