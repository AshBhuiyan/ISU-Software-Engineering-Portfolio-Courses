package onetoone.job;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "job_runs")
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "game_type", nullable = false)
    private String gameType;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private int score;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "run_nonce", unique = true)
    private String runNonce;

    @Column(name = "reward_cash")
    private double rewardCash;

    @Column(name = "reward_xp")
    private double rewardXp;

    @Column(name = "streak_bonus")
    private double streakBonus;

    @Column(name = "soft_cap_applied")
    private boolean softCapApplied;

    @Column(name = "run_at")
    private OffsetDateTime runAt;

    public JobRun() {
        this.runAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getRunNonce() { return runNonce; }
    public void setRunNonce(String runNonce) { this.runNonce = runNonce; }

    public double getRewardCash() { return rewardCash; }
    public void setRewardCash(double rewardCash) { this.rewardCash = rewardCash; }

    public double getRewardXp() { return rewardXp; }
    public void setRewardXp(double rewardXp) { this.rewardXp = rewardXp; }

    public double getStreakBonus() { return streakBonus; }
    public void setStreakBonus(double streakBonus) { this.streakBonus = streakBonus; }

    public boolean isSoftCapApplied() { return softCapApplied; }
    public void setSoftCapApplied(boolean softCapApplied) { this.softCapApplied = softCapApplied; }

    public OffsetDateTime getRunAt() { return runAt; }
    public void setRunAt(OffsetDateTime runAt) { this.runAt = runAt; }
}

