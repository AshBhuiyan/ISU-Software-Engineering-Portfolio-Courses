package onetoone.leaderboard;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class LeaderboardWebSocketConfig implements WebSocketConfigurer {

    private final LeaderboardWebSocketHandler leaderboardHandler;

    public LeaderboardWebSocketConfig(LeaderboardWebSocketHandler leaderboardHandler) {
        this.leaderboardHandler = leaderboardHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(leaderboardHandler, "/ws/leaderboard")
                .setAllowedOriginPatterns("*"); // mobile app, so allow all origins
    }
}
