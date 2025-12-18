package onetoone.Resource;

import onetoone.Resource.Resource;
import onetoone.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    // find resource by user
    Resource findByUser(User user);

    // optionally find by user id directly
    @Query("SELECT r FROM Resource r WHERE r.user.id = :userId")
    Resource findByUserId(@Param("userId") int userId);

    Resource findById(int id);
}