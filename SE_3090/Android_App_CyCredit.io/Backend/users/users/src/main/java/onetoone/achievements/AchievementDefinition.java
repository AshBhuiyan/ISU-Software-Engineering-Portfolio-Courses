package onetoone.achievements;

import jakarta.persistence.*;

@Entity
@Table(name = "achievement_definitions")
public class AchievementDefinition {

    @Id
    @Column(name = "achievement_id")
    private String achievementId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "reward_cash")
    private double rewardCash;

    @Column(name = "reward_xp")
    private double rewardXp;

    @Column(name = "badge_name")
    private String badgeName;

    public AchievementDefinition() {}

    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getRewardCash() { return rewardCash; }
    public void setRewardCash(double rewardCash) { this.rewardCash = rewardCash; }

    public double getRewardXp() { return rewardXp; }
    public void setRewardXp(double rewardXp) { this.rewardXp = rewardXp; }

    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) { this.badgeName = badgeName; }
}

