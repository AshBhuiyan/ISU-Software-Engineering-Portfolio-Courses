package onetoone.leaderboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final LeaderboardScoreRepository repository;
    private final LeaderboardWebSocketHandler wsHandler;

    public LeaderboardController(LeaderboardScoreRepository repository,
                                 LeaderboardWebSocketHandler wsHandler) {
        this.repository = repository;
        this.wsHandler = wsHandler;
    }
    @Operation(
            summary = "Get top leaderboard scores",
            description = "Returns the top N leaderboard entries ordered by score."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top leaderboard returned successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/top")
    public ResponseEntity<List<Map<String, Object>>> top(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        List<LeaderboardScore> top = repository.findTop20ByOrderByScoreDescUpdatedAtAsc();
        if (limit > 0 && top.size() > limit) {
            top = top.subList(0, limit);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        int rank = 1;
        for (LeaderboardScore s : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("userId", s.getUserId());
            m.put("displayName", s.getDisplayName());
            m.put("score", s.getScore());
            m.put("updatedAt", s.getUpdatedAt().toString());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }
    @Operation(
            summary = "Add to a user's score",
            description = "Adds a score delta to a user. Creates the user if they do not exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addScore(@RequestBody UpdateRequest req) {
        if (req == null || req.userId == null || req.userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String userId = req.userId.trim();
        String displayName = (req.displayName == null || req.displayName.isBlank())
                ? ("User " + userId)
                : req.displayName.trim();
        int delta = req.delta;

        LeaderboardScore score = repository.findByUserId(userId).orElseGet(() -> {
            LeaderboardScore s = new LeaderboardScore();
            s.setUserId(userId);
            s.setDisplayName(displayName);
            s.setScore(0);
            s.setUpdatedAt(Instant.now());
            return s;
        });

        // Update fields
        score.setDisplayName(displayName); // refresh name if changed
        score.setScore(Math.max(0, score.getScore() + delta));
        score.setUpdatedAt(Instant.now());

        repository.save(score);

        // Broadcast to all WebSocket clients
        wsHandler.broadcastLeaderboard();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", score.getUserId());
        out.put("displayName", score.getDisplayName());
        out.put("score", score.getScore());
        out.put("updatedAt", score.getUpdatedAt().toString());

        return ResponseEntity.ok(out);
    }
    @Operation(
            summary = "Set a user's score",
            description = "Replaces a user's current score with a new specific value."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/set")
    public ResponseEntity<Map<String, Object>> setScore(@RequestBody SetRequest req) {
        if (req == null || req.userId == null || req.userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String userId = req.userId.trim();
        String displayName = (req.displayName == null || req.displayName.isBlank())
                ? ("User " + userId)
                : req.displayName.trim();
        int newScore = Math.max(0, req.score);

        LeaderboardScore score = repository.findByUserId(userId).orElseGet(() -> {
            LeaderboardScore s = new LeaderboardScore();
            s.setUserId(userId);
            s.setDisplayName(displayName);
            s.setScore(0);
            s.setUpdatedAt(Instant.now());
            return s;
        });

        score.setDisplayName(displayName);
        score.setScore(newScore);
        score.setUpdatedAt(Instant.now());

        repository.save(score);
        wsHandler.broadcastLeaderboard();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", score.getUserId());
        out.put("displayName", score.getDisplayName());
        out.put("score", score.getScore());
        out.put("updatedAt", score.getUpdatedAt().toString());

        return ResponseEntity.ok(out);
    }
    @Operation(
            summary = "Add score using query parameters",
            description = "Adds score to a user using request parameters instead of a request body."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/addForUser")
    public ResponseEntity<Map<String, Object>> addScoreForUser(
            @RequestParam("userId") String userId,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam("delta") int delta
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String effectiveName = (displayName == null || displayName.isBlank())
            ? ("User " + userId)
            : displayName.trim();

        LeaderboardScore score = repository.findByUserId(userId).orElseGet(() -> {
            LeaderboardScore s = new LeaderboardScore();
            s.setUserId(userId);
            s.setDisplayName(effectiveName);
            s.setScore(0);
            s.setUpdatedAt(Instant.now());
            return s;
        });

        score.setDisplayName(effectiveName);
        score.setScore(Math.max(0, score.getScore() + delta));
        score.setUpdatedAt(Instant.now());
        repository.save(score);

        wsHandler.broadcastLeaderboard();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", score.getUserId());
        out.put("displayName", score.getDisplayName());
        out.put("score", score.getScore());
        out.put("updatedAt", score.getUpdatedAt().toString());
        return ResponseEntity.ok(out);
    }

    // --- DTOs for requests ---

    public static class UpdateRequest {
        public String userId;
        public String displayName;
        public int delta;
    }

    public static class SetRequest {
        public String userId;
        public String displayName;
        public int score;
    }
}
