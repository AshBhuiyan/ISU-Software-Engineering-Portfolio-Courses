package onetoone.achievements;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_achievements")
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id")
    private AchievementDefinition achievement;

    @Column(name = "unlocked_at")
    private OffsetDateTime unlockedAt;

    public UserAchievement() {
        this.unlockedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AchievementDefinition getAchievement() { return achievement; }
    public void setAchievement(AchievementDefinition achievement) { this.achievement = achievement; }

    public OffsetDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(OffsetDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}

