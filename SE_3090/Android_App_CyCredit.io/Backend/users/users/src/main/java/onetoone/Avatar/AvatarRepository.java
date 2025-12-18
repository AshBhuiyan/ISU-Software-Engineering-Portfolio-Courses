package onetoone.Avatar;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarRepository extends JpaRepository<Avatar, Integer> {
    Avatar findByUser(User user);
    Avatar findByUserId(int userId);
    Avatar findById(int id);
}
