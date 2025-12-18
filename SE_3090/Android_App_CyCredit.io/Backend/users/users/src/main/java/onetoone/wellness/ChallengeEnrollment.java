package onetoone.wellness;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "challenge_enrollments")
public class ChallengeEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id")
    private WellnessChallenge challenge;

    @Column(name = "progress_count")
    private int progressCount;

    @Column(name = "target_count")
    private int targetCount;

    @Column(nullable = false)
    private String status; // ENROLLED, COMPLETED, CLAIMED

    @Column(name = "enrolled_at")
    private OffsetDateTime enrolledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    public ChallengeEnrollment() {
        this.status = "ENROLLED";
        this.enrolledAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public WellnessChallenge getChallenge() { return challenge; }
    public void setChallenge(WellnessChallenge challenge) { this.challenge = challenge; }

    public int getProgressCount() { return progressCount; }
    public void setProgressCount(int progressCount) { this.progressCount = progressCount; }

    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(OffsetDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public OffsetDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(OffsetDateTime claimedAt) { this.claimedAt = claimedAt; }
}

