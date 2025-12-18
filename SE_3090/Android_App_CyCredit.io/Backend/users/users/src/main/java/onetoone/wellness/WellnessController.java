package onetoone.wellness;

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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * WellnessController handles wellness challenges.
 * Claims are idempotent: second claim returns same receipt.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/wellness")
public class WellnessController {

    private final WellnessChallengeRepository challengeRepo;
    private final ChallengeEnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final BillingService billingService;
    private final GameService gameService;
    private final AchievementsController achievementsController;
    private final GameConfig config;

    public WellnessController(WellnessChallengeRepository challengeRepo,
                             ChallengeEnrollmentRepository enrollmentRepo,
                             UserRepository userRepo,
                             ResourceRepository resourceRepo,
                             BillingService billingService,
                             GameService gameService,
                             AchievementsController achievementsController,
                             GameConfig config) {
        this.challengeRepo = challengeRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
        this.billingService = billingService;
        this.gameService = gameService;
        this.achievementsController = achievementsController;
        this.config = config;
        seedChallenges();
    }

    private void seedChallenges() {
        if (challengeRepo.count() == 0) {
            WellnessChallenge challenge = new WellnessChallenge();
            challenge.setTitle("Weekly Wellness Challenge");
            challenge.setDescription("Complete 3 wellness activities this week");
            challenge.setTargetCount(3);
            challenge.setRewardCash(config.getWellnessRewardMoney());
            challenge.setRewardXp(15.0);
            challenge.setStartDate(LocalDate.now());
            challenge.setEndDate(LocalDate.now().plusWeeks(1));
            challenge.setActive(true);
            challengeRepo.save(challenge);
        }
    }

    @GetMapping("/challenges/active")
    public ResponseEntity<?> getActiveChallenge() {
        Optional<WellnessChallenge> challenge = challengeRepo.findFirstByIsActiveTrue();
        if (challenge.isEmpty()) {
            return ResponseEntity.ok(new HashMap<>());
        }
        return ResponseEntity.ok(toChallengeDTO(challenge.get()));
    }

    @PostMapping("/challenges/{id}/enroll")
    @Transactional
    public ResponseEntity<?> enroll(
            @PathVariable Long id,
            @RequestParam int userId) {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            WellnessChallenge challenge = challengeRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Challenge not found: " + id));

            // Check if already enrolled (idempotent)
            Optional<ChallengeEnrollment> existing = enrollmentRepo.findByUser_IdAndChallenge_Id(userId, id);
            if (existing.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Already enrolled");
                response.put("enrollmentId", existing.get().getId());
                return ResponseEntity.ok(response);
            }

            ChallengeEnrollment enrollment = new ChallengeEnrollment();
            enrollment.setUser(user);
            enrollment.setChallenge(challenge);
            enrollment.setProgressCount(0);
            enrollment.setTargetCount(challenge.getTargetCount());
            enrollment.setStatus("ENROLLED");
            enrollmentRepo.save(enrollment);

            Map<String, Object> response = new HashMap<>();
            response.put("enrollmentId", enrollment.getId());
            response.put("status", enrollment.getStatus());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Enroll error: " + e.getMessage()));
        }
    }

    @PostMapping("/enrollments/{id}/submit")
    @Transactional
    public ResponseEntity<?> submitProgress(
            @PathVariable Long id,
            @RequestParam int userId,
            @RequestBody Map<String, Object> payload) {
        try {
            ChallengeEnrollment enrollment = enrollmentRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

            if (enrollment.getUser().getId() != userId) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Unauthorized"));
            }

            boolean passed = (Boolean) payload.getOrDefault("passed", false);
            if (passed) {
                enrollment.setProgressCount(enrollment.getProgressCount() + 1);
                if (enrollment.getProgressCount() >= enrollment.getTargetCount()) {
                    enrollment.setStatus("COMPLETED");
                    enrollment.setCompletedAt(OffsetDateTime.now());
                    // Unlock FIRST_WEEK achievement if this is the first completed challenge
                    List<ChallengeEnrollment> completed = enrollmentRepo.findByUser_IdOrderByEnrolledAtDesc(userId);
                    long completedCount = completed.stream()
                            .filter(e -> "COMPLETED".equals(e.getStatus()) || "CLAIMED".equals(e.getStatus()))
                            .count();
                    if (completedCount == 1) {
                        achievementsController.unlockAchievement(userId, "FIRST_WEEK");
                    }
                }
                enrollmentRepo.save(enrollment);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("progressCount", enrollment.getProgressCount());
            response.put("targetCount", enrollment.getTargetCount());
            response.put("status", enrollment.getStatus());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Submit error: " + e.getMessage()));
        }
    }

    @PostMapping("/enrollments/{id}/claim")
    @Transactional
    public ResponseEntity<?> claimReward(
            @PathVariable Long id,
            @RequestParam int userId) {
        try {
            ChallengeEnrollment enrollment = enrollmentRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

            if (enrollment.getUser().getId() != userId) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Unauthorized"));
            }

            // Idempotent claim - if already claimed, return same receipt
            if ("CLAIMED".equals(enrollment.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("alreadyClaimed", true);
                response.put("rewardCash", enrollment.getChallenge().getRewardCash());
                response.put("rewardXp", enrollment.getChallenge().getRewardXp());
                return ResponseEntity.ok(response);
            }

            if (!"COMPLETED".equals(enrollment.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.NOT_COMPLETED, "Challenge not completed yet"));
            }

            // Grant rewards (first time only)
            WellnessChallenge challenge = enrollment.getChallenge();
            Resource res = resourceRepo.findByUserId(userId);
            if (res != null) {
                res.setMoney(Money.add(res.getMoney(), challenge.getRewardCash()));
                resourceRepo.save(res);

                // Create reward transaction with positive amount (normalized model)
                billingService.createTransaction(userId, "State Gym - Wellness Reward",
                        challenge.getRewardCash(), "Reward", Transaction.TransactionType.REWARD);
            }

            enrollment.setStatus("CLAIMED");
            enrollment.setClaimedAt(OffsetDateTime.now());
            enrollmentRepo.save(enrollment);

            Map<String, Object> response = new HashMap<>();
            response.put("rewardCash", challenge.getRewardCash());
            response.put("rewardXp", challenge.getRewardXp());
            response.put("badge", "Wellness Champion");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Claim error: " + e.getMessage()));
        }
    }

    @GetMapping("/enrollments/me")
    public ResponseEntity<List<Map<String, Object>>> getMyEnrollments(@RequestParam int userId) {
        List<ChallengeEnrollment> enrollments = enrollmentRepo.findByUser_IdOrderByEnrolledAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChallengeEnrollment e : enrollments) {
            result.add(toEnrollmentDTO(e));
        }
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> toChallengeDTO(WellnessChallenge challenge) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", challenge.getId());
        dto.put("title", challenge.getTitle());
        dto.put("description", challenge.getDescription());
        dto.put("targetCount", challenge.getTargetCount());
        dto.put("rewardCash", challenge.getRewardCash());
        dto.put("rewardXp", challenge.getRewardXp());
        dto.put("startDate", challenge.getStartDate().toString());
        dto.put("endDate", challenge.getEndDate().toString());
        return dto;
    }

    private Map<String, Object> toEnrollmentDTO(ChallengeEnrollment enrollment) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", enrollment.getId());
        dto.put("challengeId", enrollment.getChallenge().getId());
        dto.put("progressCount", enrollment.getProgressCount());
        dto.put("targetCount", enrollment.getTargetCount());
        dto.put("status", enrollment.getStatus());
        dto.put("enrolledAt", enrollment.getEnrolledAt().toString());
        if (enrollment.getCompletedAt() != null) {
            dto.put("completedAt", enrollment.getCompletedAt().toString());
        }
        return dto;
    }
}
