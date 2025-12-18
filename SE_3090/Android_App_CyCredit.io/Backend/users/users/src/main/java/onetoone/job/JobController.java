package onetoone.job;

import onetoone.achievements.AchievementsController;
import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * JobController handles job runs with idempotency via runNonce.
 * Enforces MIN_DURATION_MS, calculates streak bonuses, and applies soft caps.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/job")
public class JobController {

    private final JobRunRepository runRepo;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final BillingService billingService;
    private final GameService gameService;
    private final AchievementsController achievementsController;
    private final GameConfig config;

    // Base payouts (can be moved to config later)
    private static final double EASY_PAYOUT = 6.0;
    private static final double MEDIUM_PAYOUT = 10.0;
    private static final double HARD_PAYOUT = 16.0;
    private static final double EASY_XP = 6.0;
    private static final double MEDIUM_XP = 10.0;
    private static final double HARD_XP = 16.0;

    public JobController(JobRunRepository runRepo, UserRepository userRepo,
                        ResourceRepository resourceRepo, BillingService billingService,
                        GameService gameService, AchievementsController achievementsController,
                        GameConfig config) {
        this.runRepo = runRepo;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
        this.billingService = billingService;
        this.gameService = gameService;
        this.achievementsController = achievementsController;
        this.config = config;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("easyPayout", EASY_PAYOUT);
        response.put("mediumPayout", MEDIUM_PAYOUT);
        response.put("hardPayout", HARD_PAYOUT);
        response.put("easyXp", EASY_XP);
        response.put("mediumXp", MEDIUM_XP);
        response.put("hardXp", HARD_XP);
        response.put("softCapThreshold", config.getJobSoftCapPasses());
        response.put("softCapReduction", config.getJobSoftCapReduction());
        response.put("minDurationMs", config.getJobMinDurationMs());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/run")
    @Transactional
    public ResponseEntity<?> submitRun(
            @RequestParam int userId,
            @RequestBody Map<String, Object> payload) {
        try {
            // Check turns
            if (!gameService.consumeTurn(userId)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.NO_TURNS, 
                            "You're out of turns for this month. End the month to continue."));
            }

            String gameType = (String) payload.get("gameType");
            String difficulty = (String) payload.get("difficulty");
            int score = ((Number) payload.get("score")).intValue();
            boolean passed = (Boolean) payload.get("passed");
            long durationMs = ((Number) payload.get("durationMs")).longValue();
            String runNonce = (String) payload.get("runNonce");

            // Validate runNonce is provided
            if (runNonce == null || runNonce.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "runNonce is required"));
            }

            // Anti-spam: enforce minimum duration
            if (durationMs < config.getJobMinDurationMs()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.INVALID_DURATION, 
                            String.format("Run duration must be at least %d ms", config.getJobMinDurationMs())));
            }

            // Check for duplicate nonce (idempotency)
            if (runRepo.findByRunNonce(runNonce).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.DUPLICATE_NONCE, 
                            "This run has already been processed"));
            }

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            JobRun run = new JobRun();
            run.setUser(user);
            run.setGameType(gameType);
            run.setDifficulty(difficulty);
            run.setScore(score);
            run.setPassed(passed);
            run.setDurationMs(durationMs);
            run.setRunNonce(runNonce);
            run.setRunAt(OffsetDateTime.now());

            double rewardCash = 0.0;
            double rewardXp = 0.0;
            double streakBonus = 0.0;
            boolean softCapApplied = false;
            int streak = 0; // Declare streak outside if block so it's accessible later

            if (passed) {
                // Calculate base payout
                double baseCash = 0.0;
                double baseXp = 0.0;
                switch (difficulty.toUpperCase()) {
                    case "EASY":
                        baseCash = EASY_PAYOUT;
                        baseXp = EASY_XP;
                        break;
                    case "MEDIUM":
                        baseCash = MEDIUM_PAYOUT;
                        baseXp = MEDIUM_XP;
                        break;
                    case "HARD":
                        baseCash = HARD_PAYOUT;
                        baseXp = HARD_XP;
                        break;
                }

                // Calculate streak bonus (count consecutive passes, excluding current run)
                List<JobRun> recentRuns = runRepo.findByUser_IdOrderByRunAtDesc(userId);
                streak = 0;
                for (JobRun r : recentRuns) {
                    if (r.isPassed()) {
                        streak++;
                    } else {
                        break;
                    }
                }
                // Apply streak bonus (max streak from config)
                int effectiveStreak = Math.min(streak, config.getJobStreakMax());
                streakBonus = effectiveStreak * config.getJobStreakBonusPercent();
                rewardCash = Money.multiply(baseCash, 1.0 + streakBonus);
                rewardXp = Money.multiply(baseXp, 1.0 + streakBonus);

                // Apply soft cap
                OffsetDateTime oneHourAgo = OffsetDateTime.now().minus(1, ChronoUnit.HOURS);
                List<JobRun> recentPasses = runRepo.findByUser_IdAndPassedTrueAndRunAtAfter(
                        userId, oneHourAgo);
                if (recentPasses.size() >= config.getJobSoftCapPasses()) {
                    softCapApplied = true;
                    rewardCash = Money.multiply(rewardCash, 1.0 - config.getJobSoftCapReduction());
                    rewardXp = Money.multiply(rewardXp, 1.0 - config.getJobSoftCapReduction());
                }

                // Round rewards
                rewardCash = Money.round2(rewardCash);
                rewardXp = Money.round2(rewardXp);

                // Grant rewards
                Resource res = resourceRepo.findByUserId(userId);
                if (res != null) {
                    res.setMoney(Money.add(res.getMoney(), rewardCash));
                    resourceRepo.save(res);

                    // Create income transaction with positive amount (normalized model)
                    billingService.createTransaction(userId, "Curtiss Work Arcade - " + gameType,
                            rewardCash, "Income", Transaction.TransactionType.INCOME);
                }
            }

            run.setRewardCash(rewardCash);
            run.setRewardXp(rewardXp);
            run.setStreakBonus(streakBonus);
            run.setSoftCapApplied(softCapApplied);
            runRepo.save(run);

            // Check for achievement milestones (after saving, so current run is included)
            if (passed) {
                List<JobRun> allPasses = runRepo.findByUser_IdAndPassedTrueAndRunAtAfter(
                        userId, OffsetDateTime.now().minusYears(1)); // All-time passes
                int totalPasses = allPasses.size();
                if (totalPasses == 10) {
                    achievementsController.unlockAchievement(userId, "JOB_10_PASSES");
                } else if (totalPasses == 50) {
                    achievementsController.unlockAchievement(userId, "JOB_50_PASSES");
                }
                
                // Check streak milestones (streak includes current run now)
                int currentStreak = streak + 1; // +1 for the run we just saved
                if (currentStreak == 5) {
                    achievementsController.unlockAchievement(userId, "JOB_STREAK_5");
                } else if (currentStreak == 10) {
                    achievementsController.unlockAchievement(userId, "JOB_STREAK_10");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("rewardCash", rewardCash);
            response.put("rewardXp", rewardXp);
            response.put("streakBonus", streakBonus);
            response.put("softCapApplied", softCapApplied);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Run error: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam int userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<JobRun> runs = runRepo.findByUser_IdOrderByRunAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (JobRun run : runs) {
            if (result.size() >= limit) break;
            result.add(toRunDTO(run));
        }
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> toRunDTO(JobRun run) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", run.getId());
        dto.put("gameType", run.getGameType());
        dto.put("difficulty", run.getDifficulty());
        dto.put("score", run.getScore());
        dto.put("passed", run.isPassed());
        dto.put("durationMs", run.getDurationMs());
        dto.put("rewardCash", run.getRewardCash());
        dto.put("rewardXp", run.getRewardXp());
        dto.put("streakBonus", run.getStreakBonus());
        dto.put("softCapApplied", run.isSoftCapApplied());
        dto.put("runAt", run.getRunAt().toString());
        return dto;
    }
}
