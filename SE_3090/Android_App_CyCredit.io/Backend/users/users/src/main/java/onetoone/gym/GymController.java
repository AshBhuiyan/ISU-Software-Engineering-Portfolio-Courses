package onetoone.gym;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.billing.BillingService;
import onetoone.billing.Transaction;
import onetoone.util.ApiError;
import onetoone.util.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * GymController handles State Gym functionality:
 * - Membership tiers (Basic, Premium, VIP)
 * - Workout mini-games with rewards based on tier
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/gym")
public class GymController {

    // Membership prices (per month/cycle)
    private static final double BASIC_PRICE = 5.0;
    private static final double PREMIUM_PRICE = 15.0;
    private static final double VIP_PRICE = 30.0;

    // Base workout rewards (before multiplier)
    private static final double WEIGHTLIFTING_BASE = 4.0;
    private static final double TREADMILL_BASE = 3.0;
    private static final double YOGA_BASE = 3.5;
    private static final double JUMPROPE_BASE = 2.5;

    // XP rewards
    private static final double BASE_XP = 5.0;

    private final GymMembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final BillingService billingService;

    public GymController(GymMembershipRepository membershipRepo,
                        UserRepository userRepo,
                        ResourceRepository resourceRepo,
                        BillingService billingService) {
        this.membershipRepo = membershipRepo;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
        this.billingService = billingService;
    }

    /**
     * Get membership status for a user
     */
    @GetMapping("/membership")
    public ResponseEntity<?> getMembership(@RequestParam int userId) {
        try {
            GymMembership membership = getOrCreateMembership(userId);
            return ResponseEntity.ok(toMembershipDTO(membership));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * Subscribe to or upgrade membership
     */
    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<?> subscribe(
            @RequestParam int userId,
            @RequestParam String tier) {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            Resource resource = resourceRepo.findByUserId(userId);
            if (resource == null) {
                throw new RuntimeException("Resource not found for user: " + userId);
            }

            GymMembership.MembershipTier newTier;
            double price;
            
            switch (tier.toUpperCase()) {
                case "BASIC":
                    newTier = GymMembership.MembershipTier.BASIC;
                    price = BASIC_PRICE;
                    break;
                case "PREMIUM":
                    newTier = GymMembership.MembershipTier.PREMIUM;
                    price = PREMIUM_PRICE;
                    break;
                case "VIP":
                    newTier = GymMembership.MembershipTier.VIP;
                    price = VIP_PRICE;
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(new ApiError(ApiError.BAD_REQUEST, "Invalid tier. Use BASIC, PREMIUM, or VIP."));
            }

            // Check if user can afford it
            if (resource.getMoney() < price) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.INSUFFICIENT_FUNDS, 
                            String.format("Not enough money. Need $%.2f, have $%.2f", price, resource.getMoney())));
            }

            // Deduct payment
            resource.setMoney(Money.subtract(resource.getMoney(), price));
            resourceRepo.save(resource);

            // Create transaction
            billingService.applyCharge(userId, "State Gym - " + tier + " Membership", 
                    price, "Subscription", OffsetDateTime.now(), UUID.randomUUID().toString());

            // Update membership
            GymMembership membership = getOrCreateMembership(userId);
            membership.setTier(newTier);
            membership.setSubscribedAt(OffsetDateTime.now());
            membership.setExpiresAt(OffsetDateTime.now().plusDays(30)); // 30 days
            membershipRepo.save(membership);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tier", newTier.name());
            response.put("price", price);
            response.put("expiresAt", membership.getExpiresAt().toString());
            response.put("message", String.format("Welcome to %s membership! Enjoy %.0fx rewards!", 
                    newTier.name(), membership.getRewardMultiplier()));
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * Complete a workout and earn rewards
     */
    @PostMapping("/workout")
    @Transactional
    public ResponseEntity<?> completeWorkout(
            @RequestParam int userId,
            @RequestBody Map<String, Object> payload) {
        try {
            GymMembership membership = getOrCreateMembership(userId);
            
            // Check if membership is active
            if (!membership.isActive()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.MEMBERSHIP_REQUIRED, 
                            "You need an active gym membership to workout. Subscribe first!"));
            }

            // Check daily workout limit
            String today = LocalDate.now().toString();
            if (!today.equals(membership.getLastWorkoutDate())) {
                // New day - reset counter
                membership.setWorkoutsToday(0);
                membership.setLastWorkoutDate(today);
            }

            if (membership.getWorkoutsToday() >= membership.getMaxWorkoutsPerDay()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.DAILY_LIMIT_REACHED, 
                            String.format("Daily workout limit reached (%d/%d). Come back tomorrow or upgrade your membership!", 
                                membership.getWorkoutsToday(), membership.getMaxWorkoutsPerDay())));
            }

            // Get workout details
            String workoutType = (String) payload.getOrDefault("workoutType", "WEIGHTLIFTING");
            int score = ((Number) payload.getOrDefault("score", 0)).intValue();
            long durationMs = ((Number) payload.getOrDefault("durationMs", 0)).longValue();
            boolean passed = (Boolean) payload.getOrDefault("passed", false);

            if (!passed) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Workout failed. Try again!");
                response.put("rewardCash", 0);
                response.put("rewardXp", 0);
                return ResponseEntity.ok(response);
            }

            // Calculate base reward based on workout type
            double baseReward;
            switch (workoutType.toUpperCase()) {
                case "WEIGHTLIFTING":
                    baseReward = WEIGHTLIFTING_BASE;
                    break;
                case "TREADMILL":
                    baseReward = TREADMILL_BASE;
                    break;
                case "YOGA":
                    baseReward = YOGA_BASE;
                    break;
                case "JUMPROPE":
                    baseReward = JUMPROPE_BASE;
                    break;
                default:
                    baseReward = 3.0;
            }

            // Apply membership multiplier
            double rewardCash = Money.round2(baseReward * membership.getRewardMultiplier());
            double rewardXp = Money.round2(BASE_XP * membership.getXpMultiplier());

            // Score bonus (up to +50% for perfect score)
            double scoreBonus = Math.min(score / 200.0, 0.5);
            rewardCash = Money.round2(rewardCash * (1 + scoreBonus));
            rewardXp = Money.round2(rewardXp * (1 + scoreBonus));

            // Grant rewards
            Resource resource = resourceRepo.findByUserId(userId);
            if (resource != null) {
                resource.setMoney(Money.add(resource.getMoney(), rewardCash));
                resourceRepo.save(resource);

                // Create reward transaction
                billingService.createTransaction(userId, 
                        "State Gym - " + workoutType + " Workout",
                        rewardCash, "Reward", Transaction.TransactionType.REWARD);
            }

            // Update membership stats
            membership.setWorkoutsToday(membership.getWorkoutsToday() + 1);
            membership.setTotalWorkouts(membership.getTotalWorkouts() + 1);
            membershipRepo.save(membership);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("workoutType", workoutType);
            response.put("score", score);
            response.put("rewardCash", rewardCash);
            response.put("rewardXp", rewardXp);
            response.put("multiplier", membership.getRewardMultiplier());
            response.put("workoutsToday", membership.getWorkoutsToday());
            response.put("maxWorkoutsPerDay", membership.getMaxWorkoutsPerDay());
            response.put("totalWorkouts", membership.getTotalWorkouts());
            response.put("message", String.format("Great workout! +$%.2f (%.0fx multiplier)", 
                    rewardCash, membership.getRewardMultiplier()));
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * Get available workout types and their base rewards
     */
    @GetMapping("/workouts")
    public ResponseEntity<?> getWorkoutTypes(@RequestParam int userId) {
        GymMembership membership = getOrCreateMembership(userId);
        double multiplier = membership.getRewardMultiplier();

        List<Map<String, Object>> workouts = new ArrayList<>();
        
        workouts.add(createWorkoutInfo("WEIGHTLIFTING", "Weight Lifting", 
                "Tap rapidly to lift the barbell!", WEIGHTLIFTING_BASE, multiplier, 15));
        workouts.add(createWorkoutInfo("TREADMILL", "Treadmill Run", 
                "Hold to run, release to rest. Don't overheat!", TREADMILL_BASE, multiplier, 20));
        workouts.add(createWorkoutInfo("YOGA", "Yoga Balance", 
                "Tap at the right moment to hold poses!", YOGA_BASE, multiplier, 12));
        workouts.add(createWorkoutInfo("JUMPROPE", "Jump Rope", 
                "Tap to the rhythm to skip rope!", JUMPROPE_BASE, multiplier, 18));

        Map<String, Object> response = new HashMap<>();
        response.put("workouts", workouts);
        response.put("membership", toMembershipDTO(membership));
        return ResponseEntity.ok(response);
    }

    /**
     * Get membership tier pricing
     */
    @GetMapping("/tiers")
    public ResponseEntity<?> getTiers() {
        List<Map<String, Object>> tiers = new ArrayList<>();
        
        Map<String, Object> basic = new HashMap<>();
        basic.put("tier", "BASIC");
        basic.put("price", BASIC_PRICE);
        basic.put("rewardMultiplier", 1.0);
        basic.put("xpMultiplier", 1.0);
        basic.put("maxWorkoutsPerDay", 2);
        basic.put("description", "Perfect for beginners. 2 workouts/day, 1x rewards.");
        tiers.add(basic);

        Map<String, Object> premium = new HashMap<>();
        premium.put("tier", "PREMIUM");
        premium.put("price", PREMIUM_PRICE);
        premium.put("rewardMultiplier", 2.0);
        premium.put("xpMultiplier", 1.5);
        premium.put("maxWorkoutsPerDay", 3);
        premium.put("description", "Best value! 3 workouts/day, 2x rewards, 1.5x XP.");
        tiers.add(premium);

        Map<String, Object> vip = new HashMap<>();
        vip.put("tier", "VIP");
        vip.put("price", VIP_PRICE);
        vip.put("rewardMultiplier", 3.0);
        vip.put("xpMultiplier", 2.5);
        vip.put("maxWorkoutsPerDay", 5);
        vip.put("description", "Elite access! 5 workouts/day, 3x rewards, 2.5x XP.");
        tiers.add(vip);

        return ResponseEntity.ok(tiers);
    }

    // Helper methods
    private GymMembership getOrCreateMembership(int userId) {
        return membershipRepo.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepo.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                    GymMembership membership = new GymMembership(user);
                    return membershipRepo.save(membership);
                });
    }

    private Map<String, Object> createWorkoutInfo(String type, String name, String description, 
            double baseReward, double multiplier, int durationSeconds) {
        Map<String, Object> workout = new HashMap<>();
        workout.put("type", type);
        workout.put("name", name);
        workout.put("description", description);
        workout.put("baseReward", baseReward);
        workout.put("actualReward", Money.round2(baseReward * multiplier));
        workout.put("durationSeconds", durationSeconds);
        return workout;
    }

    private Map<String, Object> toMembershipDTO(GymMembership membership) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("tier", membership.getTier().name());
        dto.put("isActive", membership.isActive());
        dto.put("rewardMultiplier", membership.getRewardMultiplier());
        dto.put("xpMultiplier", membership.getXpMultiplier());
        dto.put("maxWorkoutsPerDay", membership.getMaxWorkoutsPerDay());
        dto.put("workoutsToday", membership.getWorkoutsToday());
        dto.put("totalWorkouts", membership.getTotalWorkouts());
        dto.put("currentStreak", membership.getCurrentStreak());
        if (membership.getExpiresAt() != null) {
            dto.put("expiresAt", membership.getExpiresAt().toString());
        }
        return dto;
    }
}

