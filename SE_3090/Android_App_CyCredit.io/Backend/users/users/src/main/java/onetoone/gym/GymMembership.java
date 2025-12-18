package onetoone.gym;

import jakarta.persistence.*;
import onetoone.Users.User;
import java.time.OffsetDateTime;

@Entity
@Table(name = "gym_memberships")
public class GymMembership {

    public enum MembershipTier {
        NONE,       // No membership - can't use gym
        BASIC,      // $5/month - 1x rewards
        PREMIUM,    // $15/month - 2x rewards + bonus XP
        VIP         // $30/month - 3x rewards + exclusive perks
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private MembershipTier tier = MembershipTier.NONE;

    @Column(name = "subscribed_at")
    private OffsetDateTime subscribedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "workouts_today")
    private int workoutsToday = 0;

    @Column(name = "last_workout_date")
    private String lastWorkoutDate; // YYYY-MM-DD format

    @Column(name = "total_workouts")
    private int totalWorkouts = 0;

    @Column(name = "current_streak")
    private int currentStreak = 0;

    // Constructors
    public GymMembership() {}

    public GymMembership(User user) {
        this.user = user;
        this.tier = MembershipTier.NONE;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MembershipTier getTier() { return tier; }
    public void setTier(MembershipTier tier) { this.tier = tier; }

    public OffsetDateTime getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(OffsetDateTime subscribedAt) { this.subscribedAt = subscribedAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public int getWorkoutsToday() { return workoutsToday; }
    public void setWorkoutsToday(int workoutsToday) { this.workoutsToday = workoutsToday; }

    public String getLastWorkoutDate() { return lastWorkoutDate; }
    public void setLastWorkoutDate(String lastWorkoutDate) { this.lastWorkoutDate = lastWorkoutDate; }

    public int getTotalWorkouts() { return totalWorkouts; }
    public void setTotalWorkouts(int totalWorkouts) { this.totalWorkouts = totalWorkouts; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    // Helper methods
    public double getRewardMultiplier() {
        switch (tier) {
            case VIP: return 3.0;
            case PREMIUM: return 2.0;
            case BASIC: return 1.0;
            default: return 0.0; // No membership = no rewards
        }
    }

    public double getXpMultiplier() {
        switch (tier) {
            case VIP: return 2.5;
            case PREMIUM: return 1.5;
            case BASIC: return 1.0;
            default: return 0.0;
        }
    }

    public int getMaxWorkoutsPerDay() {
        switch (tier) {
            case VIP: return 5;
            case PREMIUM: return 3;
            case BASIC: return 2;
            default: return 0;
        }
    }

    public boolean isActive() {
        return tier != MembershipTier.NONE && 
               (expiresAt == null || expiresAt.isAfter(OffsetDateTime.now()));
    }
}

