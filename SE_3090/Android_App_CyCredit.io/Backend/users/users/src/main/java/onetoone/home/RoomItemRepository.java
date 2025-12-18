package onetoone.home;

import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomItemRepository extends JpaRepository<RoomItem, Long> {
    List<RoomItem> findByUser(User user);
    List<RoomItem> findByUser_Id(Integer userId);
    int countByUser_Id(Integer userId);
}

