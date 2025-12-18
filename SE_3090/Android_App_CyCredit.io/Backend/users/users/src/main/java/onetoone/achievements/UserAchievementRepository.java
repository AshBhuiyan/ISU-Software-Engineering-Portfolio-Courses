package onetoone.achievements;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUser(User user);
    List<UserAchievement> findByUser_Id(Integer userId);
    Optional<UserAchievement> findByUser_IdAndAchievement_AchievementId(Integer userId, String achievementId);
}

