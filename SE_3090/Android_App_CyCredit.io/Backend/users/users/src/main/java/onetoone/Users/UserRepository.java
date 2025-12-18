package onetoone.Users;

import onetoone.Resource.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 
 * @author Vivek Bengre
 * 
 */ 

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u WHERE u.name = :name")
    User getByName(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.emailId = :emailId")
    java.util.List<User> findAllByEmailId(@Param("emailId") String emailId);
    
    // Get first user by email (handles duplicates)
    default User findByEmailId(String emailId) {
        java.util.List<User> users = findAllByEmailId(emailId);
        return users.isEmpty() ? null : users.get(0);
    }
    
    @Query("SELECT u FROM User u WHERE LOWER(u.emailId) = LOWER(:emailId)")
    java.util.List<User> findAllByEmailIdIgnoreCase(@Param("emailId") String emailId);
    
    // Get first user by email ignoring case (handles duplicates)
    default User findByEmailIdIgnoreCase(String emailId) {
        java.util.List<User> users = findAllByEmailIdIgnoreCase(emailId);
        return users.isEmpty() ? null : users.get(0);
    }

    @Transactional
    void deleteById(int id);

    @Query("SELECT u FROM User u WHERE u.resource.id = :resourceId")
    User findByResourceId(@Param("resourceId") int resourceId);
}
