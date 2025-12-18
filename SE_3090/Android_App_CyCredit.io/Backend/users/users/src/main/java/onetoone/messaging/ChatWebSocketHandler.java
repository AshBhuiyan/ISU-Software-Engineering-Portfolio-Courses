package onetoone.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import onetoone.messaging.chat.ChatMessage;
import onetoone.messaging.chat.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired ChatMessageRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    // room key: scope + ":" + channel
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String scope   = (String) session.getAttributes().getOrDefault("scope","public");
        String channel = (String) session.getAttributes().getOrDefault("channel","global");
        String key = scope + ":" + channel;
        rooms.computeIfAbsent(key, k -> Collections.synchronizedSet(new HashSet<>())).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String scope   = (String) session.getAttributes().getOrDefault("scope","public");
        String channel = (String) session.getAttributes().getOrDefault("channel","global");
        String user    = (String) session.getAttributes().getOrDefault("username","user");

        Map<String,Object> in = om.readValue(message.getPayload(), Map.class);
        Integer fromId = in.get("fromUserId")==null ? null : (Integer) (in.get("fromUserId") instanceof Integer ? in.get("fromUserId") : null);
        String content = String.valueOf(in.getOrDefault("content",""));

        ChatMessage m = new ChatMessage();
        m.setScope(scope);
        m.setChannel(channel);
        m.setFromUserId(fromId);
        // prefer explicit username field from payload, otherwise path var
        String uname = in.get("username")==null ? user : String.valueOf(in.get("username"));
        m.setUsername(uname);
        m.setContent(content);
        m.setCreatedAt(Instant.now());
        repo.save(m);

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("id", m.getId());
        out.put("scope", m.getScope());
        out.put("channel", m.getChannel());
        out.put("fromUserId", m.getFromUserId());
        out.put("username", m.getUsername());
        out.put("content", m.getContent());
        out.put("createdAt", m.getCreatedAt().toString());
        String json = om.writeValueAsString(out);

        String key = scope + ":" + channel;
        Set<WebSocketSession> sessions = rooms.getOrDefault(key, Collections.emptySet());
        synchronized (sessions) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String scope   = (String) session.getAttributes().getOrDefault("scope","public");
        String channel = (String) session.getAttributes().getOrDefault("channel","global");
        String key = scope + ":" + channel;
        Set<WebSocketSession> sessions = rooms.get(key);
        if (sessions != null) sessions.remove(session);
    }
}
