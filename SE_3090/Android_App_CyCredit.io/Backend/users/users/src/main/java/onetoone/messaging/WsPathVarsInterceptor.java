package onetoone.messaging;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class WsPathVarsInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest req,
                                   ServerHttpResponse res,
                                   WebSocketHandler ws,
                                   Map<String, Object> attrs) {
        // Extract templated vars from URI: /ws/chat/{scope}/{channel}/{username}
        String path = req.getURI().getPath();
        String[] parts = path.split("/");
        // naive: [..., "chat", scope, channel, username]
        if (parts.length >= 6) {
            attrs.put("scope", parts[3]);
            attrs.put("channel", parts[4]);
            attrs.put("username", parts[5]);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest req,
                               ServerHttpResponse res,
                               WebSocketHandler ws,
                               Exception ex) {
        // no-op
    }
}
