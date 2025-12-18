package onetoone.gym;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GymMembershipRepository extends JpaRepository<GymMembership, Long> {
    Optional<GymMembership> findByUser_Id(int userId);
}

