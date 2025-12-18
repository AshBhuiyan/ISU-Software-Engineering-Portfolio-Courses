package onetoone.achievements;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, String> {
    Optional<AchievementDefinition> findByAchievementId(String achievementId);
}

