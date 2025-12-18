package onetoone.library;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "library_questions")
public class LibraryQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String difficulty; // EASY, MEDIUM, HARD

    @Column(length = 1000, nullable = false)
    private String prompt;

    @Column(length = 2000)
    private String choices; // JSON array as string

    @Column(name = "correct_index")
    private int correctIndex;

    @Column(length = 1000)
    private String explanation;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public LibraryQuestion() {}

    /**
     * Constructor that accepts List<String> for choices and converts to JSON string.
     */
    public LibraryQuestion(String topic, String difficulty, String prompt, 
                          List<String> choices, int correctIndex, String explanation) {
        this.topic = topic;
        this.difficulty = difficulty;
        this.prompt = prompt;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
        try {
            this.choices = objectMapper.writeValueAsString(choices);
        } catch (JsonProcessingException e) {
            this.choices = "[]";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getChoices() { return choices; }
    public void setChoices(String choices) { this.choices = choices; }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}

