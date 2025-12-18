package onetoone.guild;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface GuildRepository extends JpaRepository<Guild, Integer> {
    List<Guild> findByNameContainingIgnoreCase(String name);
}
