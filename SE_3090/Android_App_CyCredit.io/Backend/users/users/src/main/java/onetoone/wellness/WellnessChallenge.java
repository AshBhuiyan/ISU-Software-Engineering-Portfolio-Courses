package onetoone.wellness;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "wellness_challenges")
public class WellnessChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "target_count")
    private int targetCount;

    @Column(name = "reward_cash")
    private double rewardCash;

    @Column(name = "reward_xp")
    private double rewardXp;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private boolean isActive;

    public WellnessChallenge() {
        this.isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }

    public double getRewardCash() { return rewardCash; }
    public void setRewardCash(double rewardCash) { this.rewardCash = rewardCash; }

    public double getRewardXp() { return rewardXp; }
    public void setRewardXp(double rewardXp) { this.rewardXp = rewardXp; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}

