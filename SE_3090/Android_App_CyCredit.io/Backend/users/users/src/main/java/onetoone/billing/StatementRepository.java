package onetoone.billing;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatementRepository extends JpaRepository<Statement, Long> {
    List<Statement> findByUserOrderByMonthNumberDesc(User user);
    List<Statement> findByUser_IdOrderByMonthNumberDesc(Integer userId);
    Optional<Statement> findByUser_IdAndMonthNumber(Integer userId, int monthNumber);
    Optional<Statement> findFirstByUser_IdAndStatusOrderByMonthNumberDesc(Integer userId, Statement.StatementStatus status);
}

