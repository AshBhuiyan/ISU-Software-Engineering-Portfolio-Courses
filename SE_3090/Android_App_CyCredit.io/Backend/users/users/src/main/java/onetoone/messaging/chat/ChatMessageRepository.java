package onetoone.messaging.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    @Query("SELECT m FROM ChatMessage m WHERE m.scope=:scope AND m.channel=:channel ORDER BY m.id DESC")
    List<ChatMessage> recent(@Param("scope") String scope, @Param("channel") String channel, org.springframework.data.domain.Pageable page);
}
