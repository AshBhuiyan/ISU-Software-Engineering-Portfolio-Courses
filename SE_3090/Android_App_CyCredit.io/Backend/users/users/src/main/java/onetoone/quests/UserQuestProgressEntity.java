package onetoone.quests;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_quest_progress")
public class UserQuestProgressEntity {

    @EmbeddedId
    public Id id;

    @Column(nullable = false) public String status;
    @Column(name = "progress_percent", nullable = false) public int progressPercent; // 0..100
    @Column(name = "completed_at_iso") public String completedAtIso;

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "user_id", length = 64, nullable = false) public String userId;
        @Column(name = "quest_id", length = 64, nullable = false) public String questId;

        public Id() {}
        public Id(String userId, String questId) { this.userId = userId; this.questId = questId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id)) return false;
            Id that = (Id) o;
            return Objects.equals(userId, that.userId) && Objects.equals(questId, that.questId);
        }
        @Override public int hashCode() { return Objects.hash(userId, questId); }
    }
}
