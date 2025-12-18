package onetoone.guild;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface GuildMembershipRepository extends JpaRepository<GuildMembership, Integer> {
    List<GuildMembership> findByGuildId(Integer guildId);
    Long countByGuildId(Integer guildId);
    boolean existsByGuildIdAndUserId(Integer guildId, Integer userId);
}
