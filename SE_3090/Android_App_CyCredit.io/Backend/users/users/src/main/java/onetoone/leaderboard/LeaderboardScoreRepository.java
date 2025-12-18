package onetoone.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardScoreRepository extends JpaRepository<LeaderboardScore, Long> {

    Optional<LeaderboardScore> findByUserId(String userId);

    List<LeaderboardScore> findTop20ByOrderByScoreDescUpdatedAtAsc();
}
