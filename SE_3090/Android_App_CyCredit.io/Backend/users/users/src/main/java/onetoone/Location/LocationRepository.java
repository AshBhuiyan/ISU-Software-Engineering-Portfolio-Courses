package onetoone.Location;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Integer> {
    List<Location> findByCategoryIgnoreCase(String category);
    Location findById(int id);
}
