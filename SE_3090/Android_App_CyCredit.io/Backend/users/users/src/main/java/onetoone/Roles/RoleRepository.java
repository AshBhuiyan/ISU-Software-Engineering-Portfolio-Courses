package onetoone.Roles;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findByUser(User user);
    Role findByUserId(int userId);
    Role findById(int id);
}