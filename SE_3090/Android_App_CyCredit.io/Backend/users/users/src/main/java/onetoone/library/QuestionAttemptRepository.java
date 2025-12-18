package onetoone.library;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {
    List<QuestionAttempt> findByUser(User user);
    List<QuestionAttempt> findByUser_Id(Integer userId);
    Optional<QuestionAttempt> findByUser_IdAndQuestion_Id(Integer userId, Long questionId);
    List<QuestionAttempt> findByUser_IdAndIsMasteredTrue(Integer userId);
}

