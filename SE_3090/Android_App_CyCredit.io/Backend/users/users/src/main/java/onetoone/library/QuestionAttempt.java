package onetoone.library;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "question_attempts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_id"}))
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private LibraryQuestion question;

    @Column(name = "answer_index")
    private int answerIndex;

    @Column(name = "is_correct")
    private boolean isCorrect;

    @Column(name = "is_mastered")
    private boolean isMastered;

    @Column(name = "attempted_at")
    private OffsetDateTime attemptedAt;

    public QuestionAttempt() {
        this.attemptedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LibraryQuestion getQuestion() { return question; }
    public void setQuestion(LibraryQuestion question) { this.question = question; }

    public int getAnswerIndex() { return answerIndex; }
    public void setAnswerIndex(int answerIndex) { this.answerIndex = answerIndex; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public boolean isMastered() { return isMastered; }
    public void setMastered(boolean mastered) { isMastered = mastered; }

    public OffsetDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(OffsetDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
}

