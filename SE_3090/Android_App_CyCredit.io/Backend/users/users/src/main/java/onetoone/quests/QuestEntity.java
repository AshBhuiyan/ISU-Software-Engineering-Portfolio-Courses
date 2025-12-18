package onetoone.quests;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "quests")
public class QuestEntity {
    @Id
    @Column(name = "quest_id", length = 64, nullable = false, updatable = false)
    public String questId;

    @Column(nullable = false) public String title;
    @Column(nullable = false) public String description;
    @Column(name = "reward_points", nullable = false) public int rewardPoints;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public Instant updatedAt;
}
