package onetoone.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryQuestionRepository extends JpaRepository<LibraryQuestion, Long> {
    List<LibraryQuestion> findByTopicAndDifficulty(String topic, String difficulty);
    List<LibraryQuestion> findByTopic(String topic);
    List<LibraryQuestion> findByDifficulty(String difficulty);
}

