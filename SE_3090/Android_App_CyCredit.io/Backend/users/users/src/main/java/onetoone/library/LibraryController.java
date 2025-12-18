package onetoone.library;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.achievements.AchievementsController;
import onetoone.billing.BillingService;
import onetoone.billing.Transaction;
import onetoone.config.GameConfig;
import onetoone.game.GameService;
import onetoone.util.ApiError;
import onetoone.util.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * LibraryController handles library quiz attempts.
 * First-correct per question grants reward exactly once (idempotent).
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/library")
public class LibraryController {

    private final LibraryQuestionRepository questionRepo;
    private final QuestionAttemptRepository attemptRepo;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final BillingService billingService;
    private final GameService gameService;
    private final AchievementsController achievementsController;
    private final GameConfig config;
    private final ObjectMapper objectMapper;

    public LibraryController(LibraryQuestionRepository questionRepo,
                            QuestionAttemptRepository attemptRepo,
                            UserRepository userRepo,
                            ResourceRepository resourceRepo,
                            BillingService billingService,
                            GameService gameService,
                            AchievementsController achievementsController,
                            GameConfig config) {
        this.questionRepo = questionRepo;
        this.attemptRepo = attemptRepo;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
        this.billingService = billingService;
        this.gameService = gameService;
        this.achievementsController = achievementsController;
        this.config = config;
        this.objectMapper = new ObjectMapper();
        seedQuestions();
    }

    private void seedQuestions() {
        if (questionRepo.count() == 0) {
            // Seed some sample questions
            LibraryQuestion q1 = new LibraryQuestion();
            q1.setTopic("Credit Basics");
            q1.setDifficulty("EASY");
            q1.setPrompt("What is a credit score used for?");
            q1.setChoices("[\"To determine loan eligibility\",\"To measure height\",\"To calculate taxes\",\"To track expenses\"]");
            q1.setCorrectIndex(0);
            q1.setExplanation("Credit scores help lenders assess your creditworthiness.");
            questionRepo.save(q1);

            LibraryQuestion q2 = new LibraryQuestion();
            q2.setTopic("Credit Basics");
            q2.setDifficulty("MEDIUM");
            q2.setPrompt("What is the recommended credit utilization ratio?");
            q2.setChoices("[\"Above 50%\",\"Below 30%\",\"Exactly 100%\",\"Doesn't matter\"]");
            q2.setCorrectIndex(1);
            q2.setExplanation("Keeping utilization below 30% helps maintain a good credit score.");
            questionRepo.save(q2);

            LibraryQuestion q3 = new LibraryQuestion();
            q3.setTopic("Interest Rates");
            q3.setDifficulty("HARD");
            q3.setPrompt("If you have an APR of 18%, what is the monthly interest rate?");
            q3.setChoices("[\"18%\",\"1.5%\",\"9%\",\"0.18%\"]");
            q3.setCorrectIndex(1);
            q3.setExplanation("Monthly rate = APR / 12 = 18% / 12 = 1.5%");
            questionRepo.save(q3);
        }
    }
    @Operation(
            summary = "Fetch quiz questions",
            description = "Returns a filtered list of questions based on topic, difficulty, and optional limit."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questions fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/questions")
    public ResponseEntity<List<Map<String, Object>>> getQuestions(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "50") int limit) {
        List<LibraryQuestion> questions;
        if (topic != null && difficulty != null) {
            questions = questionRepo.findByTopicAndDifficulty(topic, difficulty);
        } else if (topic != null) {
            questions = questionRepo.findByTopic(topic);
        } else if (difficulty != null) {
            questions = questionRepo.findByDifficulty(difficulty);
        } else {
            questions = questionRepo.findAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LibraryQuestion q : questions) {
            if (result.size() >= limit) break;
            result.add(toQuestionDTO(q));
        }
        return ResponseEntity.ok(result);
    }
    @Operation(
            summary = "Submit an attempt for a quiz question",
            description = "Checks if the user's answer is correct and returns explanations, rewards, and progress updates."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attempt processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or userId"),
            @ApiResponse(responseCode = "404", description = "Question not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/attempts")
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> submitAttempt(
            @RequestParam int userId,
            @RequestBody Map<String, Object> payload) {
        try {
            // Check turns
            if (!gameService.consumeTurn(userId)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.NO_TURNS, 
                            "You're out of turns for this month. End the month to continue."));
            }

            Long questionId = ((Number) payload.get("questionId")).longValue();
            int answerIndex = ((Number) payload.get("answerIndex")).intValue();

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            LibraryQuestion question = questionRepo.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));

            boolean isCorrect = answerIndex == question.getCorrectIndex();

            // Check if already mastered (idempotency check)
            Optional<QuestionAttempt> existing = attemptRepo.findByUser_IdAndQuestion_Id(userId, questionId);
            boolean alreadyMastered = existing.map(QuestionAttempt::isMastered).orElse(false);
            boolean isFirstCorrect = false;
            double rewardMoney = 0.0;
            double rewardXp = 0.0;

            QuestionAttempt attempt = existing.orElse(new QuestionAttempt());
            attempt.setUser(user);
            attempt.setQuestion(question);
            attempt.setAnswerIndex(answerIndex);
            attempt.setCorrect(isCorrect);

            if (isCorrect && !alreadyMastered) {
                // First correct answer - grant reward (idempotent: only first time)
                isFirstCorrect = true;
                attempt.setMastered(true);
                rewardMoney = config.getLibraryRewardMoney();
                rewardXp = config.getLibraryRewardXp();

                // Grant money and XP using Money utility
                Resource res = resourceRepo.findByUserId(userId);
                if (res != null) {
                    res.setMoney(Money.add(res.getMoney(), rewardMoney));
                    resourceRepo.save(res);

                    // Create reward transaction with positive amount (normalized model)
                    billingService.createTransaction(userId, "Parks Library - Quiz Reward",
                            rewardMoney, "Reward", Transaction.TransactionType.REWARD);
                }

                // Check for achievement unlocks
                List<QuestionAttempt> mastered = attemptRepo.findByUser_IdAndIsMasteredTrue(userId);
                if (mastered.size() >= 10) {
                    achievementsController.unlockAchievement(userId, "MASTER_10");
                }
            }
            // If already mastered, no reward is granted (idempotent)

            attempt.setAttemptedAt(OffsetDateTime.now());
            attemptRepo.save(attempt);

            Map<String, Object> response = new HashMap<>();
            response.put("correct", isCorrect);
            response.put("explanation", question.getExplanation());
            response.put("cleared", isFirstCorrect);
            if (isFirstCorrect) {
                Map<String, Object> reward = new HashMap<>();
                reward.put("money", rewardMoney);
                reward.put("xp", rewardXp);
                response.put("reward", reward);
            } else {
                response.put("reward", null); // No reward for subsequent correct answers
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Attempt error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Get user progress",
            description = "Returns the user's progress data, including performance and statistics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progress fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid userId"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress(@RequestParam int userId) {
        List<QuestionAttempt> mastered = attemptRepo.findByUser_IdAndIsMasteredTrue(userId);
        long totalQuestions = questionRepo.count();

        Map<String, Object> response = new HashMap<>();
        response.put("totalQuestions", totalQuestions);
        response.put("masteredQuestions", mastered.size());
        response.put("topicsCovered", mastered.stream()
                .map(a -> a.getQuestion().getTopic())
                .distinct()
                .count());
        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toQuestionDTO(LibraryQuestion q) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", q.getId());
        dto.put("topic", q.getTopic());
        dto.put("difficulty", q.getDifficulty());
        dto.put("prompt", q.getPrompt());
        try {
            List<String> choices = objectMapper.readValue(q.getChoices(), List.class);
            dto.put("choices", choices);
        } catch (JsonProcessingException e) {
            dto.put("choices", new ArrayList<>());
        }
        dto.put("correctIndex", q.getCorrectIndex());
        dto.put("explanation", q.getExplanation());
        return dto;
    }
}
