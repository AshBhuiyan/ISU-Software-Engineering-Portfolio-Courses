package onetoone.guild;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface GuildInviteRepository extends JpaRepository<GuildInvite, Integer> {
    List<GuildInvite> findByReceiverUserId(Integer receiverUserId);
}
