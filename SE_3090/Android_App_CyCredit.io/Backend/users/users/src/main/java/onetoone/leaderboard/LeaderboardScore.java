package onetoone.leaderboard;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "leaderboard_scores")
public class LeaderboardScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // --- Getters / setters ---

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
