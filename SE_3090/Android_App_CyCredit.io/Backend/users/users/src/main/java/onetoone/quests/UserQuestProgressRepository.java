package onetoone.quests;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserQuestProgressRepository
        extends JpaRepository<UserQuestProgressEntity, UserQuestProgressEntity.Id> {
    List<UserQuestProgressEntity> findByIdUserId(String userId);
}
