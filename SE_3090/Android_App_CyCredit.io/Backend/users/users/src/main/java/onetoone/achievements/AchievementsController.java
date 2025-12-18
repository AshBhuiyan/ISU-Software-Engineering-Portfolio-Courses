package onetoone.achievements;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.billing.BillingService;
import onetoone.billing.Transaction;
import onetoone.util.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * AchievementsController handles achievement unlocks.
 * Unlocks are idempotent: checking before granting prevents double unlocks.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/achievements")
public class AchievementsController {

    private final AchievementDefinitionRepository defRepo;
    private final UserAchievementRepository userAchievementRepo;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final BillingService billingService;

    public AchievementsController(AchievementDefinitionRepository defRepo,
                                 UserAchievementRepository userAchievementRepo,
                                 UserRepository userRepo,
                                 ResourceRepository resourceRepo,
                                 BillingService billingService) {
        this.defRepo = defRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
        this.billingService = billingService;
        seedAchievements();
    }

    private void seedAchievements() {
        if (defRepo.count() == 0) {
            AchievementDefinition a1 = new AchievementDefinition();
            a1.setAchievementId("FIRST_WEEK");
            a1.setTitle("First Week Complete");
            a1.setDescription("Complete your first weekly wellness challenge");
            a1.setRewardCash(10.0);
            a1.setRewardXp(10.0);
            a1.setBadgeName("Week Warrior");
            defRepo.save(a1);

            AchievementDefinition a2 = new AchievementDefinition();
            a2.setAchievementId("TOTAL_5");
            a2.setTitle("Five Challenges");
            a2.setDescription("Complete 5 wellness challenges");
            a2.setRewardCash(25.0);
            a2.setRewardXp(25.0);
            a2.setBadgeName("Challenge Master");
            defRepo.save(a2);

            AchievementDefinition a3 = new AchievementDefinition();
            a3.setAchievementId("MASTER_10");
            a3.setTitle("Library Master");
            a3.setDescription("Master 10 library questions");
            a3.setRewardCash(20.0);
            a3.setRewardXp(20.0);
            a3.setBadgeName("Scholar");
            defRepo.save(a3);

            AchievementDefinition a4 = new AchievementDefinition();
            a4.setAchievementId("JOB_10_PASSES");
            a4.setTitle("Work Hard");
            a4.setDescription("Complete 10 job runs successfully");
            a4.setRewardCash(15.0);
            a4.setRewardXp(15.0);
            a4.setBadgeName("Worker");
            defRepo.save(a4);

            AchievementDefinition a5 = new AchievementDefinition();
            a5.setAchievementId("JOB_50_PASSES");
            a5.setTitle("Workaholic");
            a5.setDescription("Complete 50 job runs successfully");
            a5.setRewardCash(50.0);
            a5.setRewardXp(50.0);
            a5.setBadgeName("Workaholic");
            defRepo.save(a5);

            AchievementDefinition a6 = new AchievementDefinition();
            a6.setAchievementId("JOB_STREAK_5");
            a6.setTitle("On a Roll");
            a6.setDescription("Get a 5-pass streak in job runs");
            a6.setRewardCash(10.0);
            a6.setRewardXp(10.0);
            a6.setBadgeName("Streak Master");
            defRepo.save(a6);

            AchievementDefinition a7 = new AchievementDefinition();
            a7.setAchievementId("JOB_STREAK_10");
            a7.setTitle("Unstoppable");
            a7.setDescription("Get a 10-pass streak in job runs");
            a7.setRewardCash(25.0);
            a7.setRewardXp(25.0);
            a7.setBadgeName("Unstoppable");
            defRepo.save(a7);
        }
    }

    @GetMapping("/defs")
    public ResponseEntity<List<Map<String, Object>>> getDefinitions() {
        List<AchievementDefinition> defs = defRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AchievementDefinition def : defs) {
            result.add(toDefinitionDTO(def));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<List<Map<String, Object>>> getMyAchievements(@RequestParam int userId) {
        List<UserAchievement> userAchievements = userAchievementRepo.findByUser_Id(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserAchievement ua : userAchievements) {
            result.add(toUserAchievementDTO(ua));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Unlock an achievement for a user (called by other services).
     * Idempotent: checks if already unlocked before granting.
     */
    @Transactional
    public void unlockAchievement(int userId, String achievementId) {
        // Check if already unlocked (idempotency)
        Optional<UserAchievement> existing = userAchievementRepo.findByUser_IdAndAchievement_AchievementId(
                userId, achievementId);
        if (existing.isPresent()) {
            return; // Already unlocked - no double grant
        }

        AchievementDefinition def = defRepo.findByAchievementId(achievementId)
                .orElse(null);
        if (def == null) {
            return; // Achievement definition not found
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(def);
        userAchievement.setUnlockedAt(OffsetDateTime.now());
        userAchievementRepo.save(userAchievement);

        // Grant rewards (only if not already unlocked)
        Resource res = resourceRepo.findByUserId(userId);
        if (res != null && def.getRewardCash() > 0) {
            res.setMoney(Money.add(res.getMoney(), def.getRewardCash()));
            resourceRepo.save(res);

            // Create reward transaction with positive amount (normalized model)
            billingService.createTransaction(userId, "Achievement: " + def.getTitle(),
                    def.getRewardCash(), "Reward", Transaction.TransactionType.REWARD);
        }
    }

    private Map<String, Object> toDefinitionDTO(AchievementDefinition def) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("achievementId", def.getAchievementId());
        dto.put("title", def.getTitle());
        dto.put("description", def.getDescription());
        dto.put("rewardCash", def.getRewardCash());
        dto.put("rewardXp", def.getRewardXp());
        dto.put("badgeName", def.getBadgeName());
        return dto;
    }

    private Map<String, Object> toUserAchievementDTO(UserAchievement ua) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("achievementId", ua.getAchievement().getAchievementId());
        dto.put("title", ua.getAchievement().getTitle());
        dto.put("description", ua.getAchievement().getDescription());
        dto.put("badgeName", ua.getAchievement().getBadgeName());
        dto.put("unlockedAt", ua.getUnlockedAt().toString());
        return dto;
    }
}
