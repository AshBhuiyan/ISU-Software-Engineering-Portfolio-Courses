package onetoone.quests;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRepository extends JpaRepository<QuestEntity, String> {}
