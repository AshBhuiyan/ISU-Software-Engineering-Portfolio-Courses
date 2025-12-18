package onetoone.leaderboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class LeaderboardWebSocketHandler extends TextWebSocketHandler {

    private final LeaderboardScoreRepository repository;
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public LeaderboardWebSocketHandler(LeaderboardScoreRepository repository,
                                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        // Send current leaderboard immediately to this new client
        sendLeaderboardToSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // For now, ignore client messages (could be used for pings later)
    }

    public void broadcastLeaderboard() {
        List<Map<String, Object>> payload = buildLeaderboardPayload();
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage msg = new TextMessage(json);

            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    try {
                        s.sendMessage(msg);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void sendLeaderboardToSession(WebSocketSession session) {
        if (!session.isOpen()) return;
        List<Map<String, Object>> payload = buildLeaderboardPayload();
        try {
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (IOException ignored) {
        }
    }

    private List<Map<String, Object>> buildLeaderboardPayload() {
        List<LeaderboardScore> top = repository.findTop20ByOrderByScoreDescUpdatedAtAsc();

        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (LeaderboardScore s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("userId", s.getUserId());
            m.put("displayName", s.getDisplayName());
            m.put("score", s.getScore());
            m.put("updatedAt", s.getUpdatedAt().toString());
            list.add(m);
        }
        return list;
    }
}
