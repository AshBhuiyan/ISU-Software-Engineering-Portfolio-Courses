package cycredit.io;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import cycredit.io.guilds.ApiClient;
import cycredit.io.util.ErrorHandler;

/**
 * State Gym - Workout to earn rewards!
 * Features:
 * - Membership tiers (Basic $5, Premium $15, VIP $30)
 * - 4 workout mini-games
 * - Rewards based on membership multiplier
 */
public class WellnessActivity extends AppCompatActivity {

    private int userId;

    // Membership UI
    private TextView membershipTierLabel;
    private TextView membershipMultiplierLabel;
    private TextView workoutsTodayLabel;
    private TextView subscribePrompt;
    private Button btnSubBasic, btnSubPremium, btnSubVip;

    // Workout selection UI
    private MaterialCardView workoutWeightlifting, workoutTreadmill, workoutYoga, workoutJumprope;
    private TextView weightliftingReward, treadmillReward, yogaReward, jumpropeReward;
    private TextView workoutStatusText;

    // Game UI
    private MaterialCardView gameContainerCard;
    private TextView gameTitle, gameInstruction, gameScoreLabel, gameTimerLabel;
    private ProgressBar gameProgressBar;
    private Button gameActionButton, gameCancelButton;

    // Stats UI
    private TextView totalWorkoutsLabel, streakLabel;
    private ProgressBar spinner;

    // State
    private String currentTier = "NONE";
    private double rewardMultiplier = 0.0;
    private int workoutsToday = 0;
    private int maxWorkoutsPerDay = 0;
    private int totalWorkouts = 0;
    private int currentStreak = 0;

    // Game state
    private boolean isGameActive = false;
    private String currentWorkoutType = "";
    private int gameScore = 0;
    private CountDownTimer gameTimer;
    
    // Game-specific state
    private int weightLiftProgress = 0; // For weight lifting
    private boolean isHoldingTreadmill = false; // For treadmill
    private int treadmillHeat = 0; // 0-100, overheating if > 80
    private int yogaZonePosition = 0; // 0-100, oscillating
    private boolean yogaMovingRight = true; // Direction for yoga
    private long lastJumpRopeTap = 0; // For rhythm timing
    private int jumpRopeBeatCount = 0; // Beat counter

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellness);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("State Gym");
        }

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) {
            userId = Session.getUserId(this);
        }
        
        if (userId <= 0) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupListeners();
        // Enable cards by default until membership loads
        setWorkoutCardsEnabled(true);
        loadMembership();
    }

    private void bindViews() {
        // Membership
        membershipTierLabel = findViewById(R.id.membership_tier_label);
        membershipMultiplierLabel = findViewById(R.id.membership_multiplier_label);
        workoutsTodayLabel = findViewById(R.id.workouts_today_label);
        subscribePrompt = findViewById(R.id.subscribe_prompt);
        btnSubBasic = findViewById(R.id.btn_sub_basic);
        btnSubPremium = findViewById(R.id.btn_sub_premium);
        btnSubVip = findViewById(R.id.btn_sub_vip);

        // Workout cards
        workoutWeightlifting = findViewById(R.id.workout_weightlifting);
        workoutTreadmill = findViewById(R.id.workout_treadmill);
        workoutYoga = findViewById(R.id.workout_yoga);
        workoutJumprope = findViewById(R.id.workout_jumprope);
        weightliftingReward = findViewById(R.id.weightlifting_reward);
        treadmillReward = findViewById(R.id.treadmill_reward);
        yogaReward = findViewById(R.id.yoga_reward);
        jumpropeReward = findViewById(R.id.jumprope_reward);
        workoutStatusText = findViewById(R.id.workout_status_text);

        // Game container
        gameContainerCard = findViewById(R.id.game_container_card);
        gameTitle = findViewById(R.id.game_title);
        gameInstruction = findViewById(R.id.game_instruction);
        gameScoreLabel = findViewById(R.id.game_score_label);
        gameTimerLabel = findViewById(R.id.game_timer_label);
        gameProgressBar = findViewById(R.id.game_progress_bar);
        gameActionButton = findViewById(R.id.game_action_button);
        gameCancelButton = findViewById(R.id.game_cancel_button);

        // Stats
        totalWorkoutsLabel = findViewById(R.id.total_workouts_label);
        streakLabel = findViewById(R.id.streak_label);
        spinner = findViewById(R.id.progress_spinner);
    }

    private void setupListeners() {
        // Back button
        findViewById(R.id.btn_back_to_map).setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, MapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Subscribe buttons
        btnSubBasic.setOnClickListener(v -> subscribe("BASIC"));
        btnSubPremium.setOnClickListener(v -> subscribe("PREMIUM"));
        btnSubVip.setOnClickListener(v -> subscribe("VIP"));

        // Workout cards
        workoutWeightlifting.setOnClickListener(v -> startWorkout("WEIGHTLIFTING"));
        workoutTreadmill.setOnClickListener(v -> startWorkout("TREADMILL"));
        workoutYoga.setOnClickListener(v -> startWorkout("YOGA"));
        workoutJumprope.setOnClickListener(v -> startWorkout("JUMPROPE"));

        // Game buttons (action button listener set dynamically per game)
        gameCancelButton.setOnClickListener(v -> cancelGame());
    }

    private void loadMembership() {
        spinner.setVisibility(View.VISIBLE);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiClient.BASE_URL + "/gym/membership?userId=" + userId,
                null,
                this::handleMembershipResponse,
                error -> {
                    spinner.setVisibility(View.GONE);
                    // If gym endpoints don't exist yet, show default state but keep cards enabled
                    // (user might have membership from backend, just endpoint not available)
                    currentTier = "NONE";
                    rewardMultiplier = 0.0;
                    workoutsToday = 0;
                    maxWorkoutsPerDay = 0;
                    updateMembershipUI();
                    // Don't disable cards - let user try to click and see what happens
                    setWorkoutCardsEnabled(true);
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void handleMembershipResponse(JSONObject response) {
        spinner.setVisibility(View.GONE);
        
        currentTier = response.optString("tier", "NONE");
        rewardMultiplier = response.optDouble("rewardMultiplier", 0.0);
        workoutsToday = response.optInt("workoutsToday", 0);
        maxWorkoutsPerDay = response.optInt("maxWorkoutsPerDay", 0);
        totalWorkouts = response.optInt("totalWorkouts", 0);
        currentStreak = response.optInt("currentStreak", 0);

        updateMembershipUI();
        updateWorkoutRewards();
    }

    private void updateMembershipUI() {
        // Tier label with color coding
        membershipTierLabel.setText(currentTier);
        switch (currentTier) {
            case "VIP":
                membershipTierLabel.setTextColor(0xFFFFD700); // Gold
                break;
            case "PREMIUM":
                membershipTierLabel.setTextColor(0xFF00C8FF); // Cyan
                break;
            case "BASIC":
                membershipTierLabel.setTextColor(0xFF3D5A80); // Blue-gray
                break;
            default:
                membershipTierLabel.setTextColor(0xFFB0C4D8); // Gray
        }

        // Multiplier
        membershipMultiplierLabel.setText(String.format(Locale.US, "%.0fx", rewardMultiplier));

        // Workouts today
        workoutsTodayLabel.setText(String.format(Locale.US, "%d/%d", workoutsToday, maxWorkoutsPerDay));

        // Stats
        totalWorkoutsLabel.setText(String.valueOf(totalWorkouts));
        streakLabel.setText(currentStreak + " days");

        // Show/hide subscribe prompt
        boolean hasMembership = !"NONE".equals(currentTier);
        subscribePrompt.setVisibility(hasMembership ? View.GONE : View.VISIBLE);
        
        // Update button states
        btnSubBasic.setAlpha(hasMembership && !"BASIC".equals(currentTier) ? 0.5f : 1.0f);
        btnSubPremium.setAlpha(hasMembership && "PREMIUM".equals(currentTier) ? 0.5f : 1.0f);
        btnSubVip.setAlpha("VIP".equals(currentTier) ? 0.5f : 1.0f);

        // Update workout status
        if (!hasMembership) {
            workoutStatusText.setText("Get a membership to start working out!");
            workoutStatusText.setTextColor(0xFFFF6B6B); // Red
            setWorkoutCardsEnabled(false);
        } else if (workoutsToday >= maxWorkoutsPerDay) {
            workoutStatusText.setText("Daily limit reached! Come back tomorrow.");
            workoutStatusText.setTextColor(0xFFFF6B6B);
            setWorkoutCardsEnabled(false);
        } else {
            int remaining = maxWorkoutsPerDay - workoutsToday;
            workoutStatusText.setText(String.format(Locale.US, 
                "%d workout%s remaining today. Tap a workout to begin!", 
                remaining, remaining == 1 ? "" : "s"));
            workoutStatusText.setTextColor(0xFF00FF88); // Green
            setWorkoutCardsEnabled(true);
        }
    }

    private void setWorkoutCardsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.4f;
        workoutWeightlifting.setAlpha(alpha);
        workoutTreadmill.setAlpha(alpha);
        workoutYoga.setAlpha(alpha);
        workoutJumprope.setAlpha(alpha);
        workoutWeightlifting.setClickable(enabled);
        workoutTreadmill.setClickable(enabled);
        workoutYoga.setClickable(enabled);
        workoutJumprope.setClickable(enabled);
    }

    private void updateWorkoutRewards() {
        // Base rewards
        double weightBase = 4.0, treadBase = 3.0, yogaBase = 3.5, jumpBase = 2.5;
        
        // Apply multiplier
        weightliftingReward.setText(String.format(Locale.US, "$%.2f", weightBase * rewardMultiplier));
        treadmillReward.setText(String.format(Locale.US, "$%.2f", treadBase * rewardMultiplier));
        yogaReward.setText(String.format(Locale.US, "$%.2f", yogaBase * rewardMultiplier));
        jumpropeReward.setText(String.format(Locale.US, "$%.2f", jumpBase * rewardMultiplier));
    }

    private void subscribe(String tier) {
        // If already at this tier or higher, show message
        if ("VIP".equals(currentTier) || 
            (currentTier.equals(tier)) ||
            ("PREMIUM".equals(currentTier) && "BASIC".equals(tier))) {
            Toast.makeText(this, "You already have this or a better membership!", Toast.LENGTH_SHORT).show();
            return;
        }

        spinner.setVisibility(View.VISIBLE);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiClient.BASE_URL + "/gym/subscribe?userId=" + userId + "&tier=" + tier,
                null,
                response -> {
                    spinner.setVisibility(View.GONE);
                    String msg = response.optString("message", "Membership activated!");
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    
                    // Refresh membership data
                    loadMembership();
                    
                    // Refresh HUD
                    HudSyncHelper.refreshHud(this, userId);
                },
                error -> {
                    spinner.setVisibility(View.GONE);
                    ErrorHandler.handleError(this, error, "Subscription failed");
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void startWorkout(String workoutType) {
        // Debug: Always show feedback when clicked
        android.util.Log.d("StateGym", "startWorkout called: " + workoutType + ", tier: " + currentTier);
        
        if ("NONE".equals(currentTier)) {
            Toast.makeText(this, "You need a gym membership first! Subscribe above.", Toast.LENGTH_LONG).show();
            return;
        }
        if (maxWorkoutsPerDay > 0 && workoutsToday >= maxWorkoutsPerDay) {
            Toast.makeText(this, "Daily workout limit reached! Come back tomorrow.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentWorkoutType = workoutType;
        isGameActive = true;
        gameScore = 0;
        
        // Reset game-specific state
        weightLiftProgress = 0;
        isHoldingTreadmill = false;
        treadmillHeat = 0;
        yogaZonePosition = 50;
        yogaMovingRight = true;
        lastJumpRopeTap = 0;
        jumpRopeBeatCount = 0;

        // Configure game based on workout type
        int duration;
        String title, instruction;
        
        switch (workoutType) {
            case "WEIGHTLIFTING":
                title = "Weight Lifting";
                instruction = "Tap RAPIDLY to lift! Fill the bar to 100%!";
                duration = 15000;
                gameActionButton.setText("LIFT!");
                gameActionButton.setOnClickListener(v -> onWeightLiftTap());
                break;
            case "TREADMILL":
                title = "Treadmill Run";
                instruction = "HOLD to run, RELEASE to cool down! Keep heat in green zone!";
                duration = 20000;
                gameActionButton.setText("HOLD TO RUN");
                gameActionButton.setOnTouchListener((v, event) -> {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        isHoldingTreadmill = true;
                        gameActionButton.setText("RUNNING...");
                        return true;
                    } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                        isHoldingTreadmill = false;
                        gameActionButton.setText("HOLD TO RUN");
                        return true;
                    }
                    return false;
                });
                break;
            case "YOGA":
                title = "Yoga Balance";
                instruction = "Tap when the indicator is in the GREEN zone (30-70)!";
                duration = 15000;
                gameActionButton.setText("TAP TO BALANCE");
                gameActionButton.setOnClickListener(v -> onYogaTap());
                break;
            case "JUMPROPE":
                title = "Jump Rope";
                instruction = "Tap in rhythm! Watch the beat indicator!";
                duration = 18000;
                gameActionButton.setText("JUMP!");
                gameActionButton.setOnClickListener(v -> onJumpRopeTap());
                break;
            default:
                title = "Workout";
                instruction = "Tap rapidly to exercise!";
                duration = 15000;
                gameActionButton.setText("GO!");
                gameActionButton.setOnClickListener(v -> onWeightLiftTap()); // Fallback to weight lifting style
        }

        gameTitle.setText(title);
        gameInstruction.setText(instruction);
        gameScoreLabel.setText("Score: 0");
        gameProgressBar.setProgress(0);
        gameProgressBar.setMax(100);

        // Show game container
        gameContainerCard.setVisibility(View.VISIBLE);

        // Start timer with game-specific updates
        gameTimerLabel.setText(String.format(Locale.US, "Time: %ds", duration / 1000));
        gameTimer = new CountDownTimer(duration, 50) { // 50ms updates for smoother gameplay
            @Override
            public void onTick(long millisUntilFinished) {
                gameTimerLabel.setText(String.format(Locale.US, "Time: %.1fs", millisUntilFinished / 1000.0));
                updateGameLogic();
            }

            @Override
            public void onFinish() {
                completeWorkout(true);
            }
        };
        gameTimer.start();
    }
    
    private void updateGameLogic() {
        if (!isGameActive) return;
        
        switch (currentWorkoutType) {
            case "WEIGHTLIFTING":
                // Progress decays slowly if not tapping
                if (weightLiftProgress > 0) {
                    weightLiftProgress = Math.max(0, weightLiftProgress - 1);
                }
                gameProgressBar.setProgress(weightLiftProgress);
                gameScoreLabel.setText("Lift Progress: " + weightLiftProgress + "%");
                if (weightLiftProgress >= 100) {
                    gameScore = 200; // Perfect score
                }
                break;
                
            case "TREADMILL":
                if (isHoldingTreadmill) {
                    // Running increases heat and score
                    treadmillHeat = Math.min(100, treadmillHeat + 2);
                    if (treadmillHeat < 80) {
                        gameScore += 1; // Only score if not overheating
                    }
                } else {
                    // Cooling down
                    treadmillHeat = Math.max(0, treadmillHeat - 3);
                }
                gameProgressBar.setProgress(treadmillHeat);
                
                // Color code: green (0-60), yellow (60-80), red (80+)
                if (treadmillHeat < 60) {
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00FF88));
                    gameScoreLabel.setText("Heat: " + treadmillHeat + " (Good!)");
                } else if (treadmillHeat < 80) {
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFFFF00));
                    gameScoreLabel.setText("Heat: " + treadmillHeat + " (Warning!)");
                } else {
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF6B6B));
                    gameScoreLabel.setText("Heat: " + treadmillHeat + " (OVERHEATING!)");
                }
                break;
                
            case "YOGA":
                // Oscillate the zone position
                if (yogaMovingRight) {
                    yogaZonePosition += 3;
                    if (yogaZonePosition >= 100) {
                        yogaZonePosition = 100;
                        yogaMovingRight = false;
                    }
                } else {
                    yogaZonePosition -= 3;
                    if (yogaZonePosition <= 0) {
                        yogaZonePosition = 0;
                        yogaMovingRight = true;
                    }
                }
                gameProgressBar.setProgress(yogaZonePosition);
                
                // Show green zone (30-70)
                int greenZoneStart = 30;
                int greenZoneEnd = 70;
                boolean inGreenZone = yogaZonePosition >= greenZoneStart && yogaZonePosition <= greenZoneEnd;
                
                if (inGreenZone) {
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00FF88));
                    gameScoreLabel.setText("Position: " + yogaZonePosition + " (TAP NOW!)");
                } else {
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF));
                    gameScoreLabel.setText("Position: " + yogaZonePosition + " (Wait for green zone)");
                }
                break;
                
            case "JUMPROPE":
                // Visual beat indicator
                jumpRopeBeatCount++;
                int beatPhase = jumpRopeBeatCount % 60; // 60 frames = ~1 beat at 50ms intervals
                
                if (beatPhase < 10) {
                    // Beat highlight
                    gameProgressBar.setProgress(100);
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00FF88));
                    gameScoreLabel.setText("Score: " + gameScore + " (TAP NOW!)");
                } else {
                    gameProgressBar.setProgress(0);
                    gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF));
                    gameScoreLabel.setText("Score: " + gameScore + " (Wait for beat...)");
                }
                break;
        }
    }

    // Weight Lifting: Rapid tapping fills progress bar
    private void onWeightLiftTap() {
        if (!isGameActive) return;
        
        weightLiftProgress = Math.min(100, weightLiftProgress + 5);
        gameScore += 2; // Score for each tap
        
        // Visual feedback
        gameActionButton.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(30)
                .withEndAction(() -> gameActionButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(30)
                        .start())
                .start();
    }
    
    // Yoga: Tap when in green zone (30-70)
    private void onYogaTap() {
        if (!isGameActive) return;
        
        int greenZoneStart = 30;
        int greenZoneEnd = 70;
        boolean inGreenZone = yogaZonePosition >= greenZoneStart && yogaZonePosition <= greenZoneEnd;
        
        if (inGreenZone) {
            // Perfect hit!
            gameScore += 20;
            gameScoreLabel.setTextColor(0xFF00FF88); // Green
            gameScoreLabel.setText("Perfect! Score: " + gameScore);
            
            // Flash green
            gameActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00FF88));
            gameActionButton.postDelayed(() -> {
                gameActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF));
            }, 200);
        } else {
            // Missed - small penalty
            gameScore = Math.max(0, gameScore - 5);
            gameScoreLabel.setTextColor(0xFFFF6B6B); // Red
            gameScoreLabel.setText("Missed! Score: " + gameScore);
        }
        
        // Animation
        gameActionButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> gameActionButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }
    
    // Jump Rope: Rhythm-based tapping
    private void onJumpRopeTap() {
        if (!isGameActive) return;
        
        long currentTime = System.currentTimeMillis();
        int beatPhase = jumpRopeBeatCount % 60;
        
        // Check if tapped on beat (within first 10 frames of beat cycle)
        if (beatPhase < 10) {
            // On beat - good!
            gameScore += 15;
            gameScoreLabel.setTextColor(0xFF00FF88); // Green
            gameScoreLabel.setText("On beat! Score: " + gameScore);
            
            // Flash green
            gameActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00FF88));
            gameActionButton.postDelayed(() -> {
                gameActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF));
            }, 200);
        } else {
            // Off beat - penalty
            gameScore = Math.max(0, gameScore - 3);
            gameScoreLabel.setTextColor(0xFFFF6B6B); // Red
            gameScoreLabel.setText("Off beat! Score: " + gameScore);
        }
        
        lastJumpRopeTap = currentTime;
        
        // Animation
        gameActionButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(50)
                .withEndAction(() -> gameActionButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .start())
                .start();
    }

    private void cancelGame() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        isGameActive = false;
        
        // Reset button state
        gameActionButton.setOnTouchListener(null);
        gameActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF));
        gameProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF00FF88));
        
        gameContainerCard.setVisibility(View.GONE);
        Toast.makeText(this, "Workout cancelled", Toast.LENGTH_SHORT).show();
    }

    private void completeWorkout(boolean ignoredParam) {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        isGameActive = false;
        
        // Reset button state
        gameActionButton.setOnTouchListener(null);
        gameActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00C8FF));

        // Determine if passed based on game type and score
        int minScore;
        boolean didPass;
        switch (currentWorkoutType) {
            case "WEIGHTLIFTING":
                minScore = 100; // Need to fill progress bar (100%)
                didPass = weightLiftProgress >= 100 || gameScore >= 100;
                break;
            case "TREADMILL":
                minScore = 150; // Need to maintain good heat for a while
                didPass = gameScore >= minScore && treadmillHeat < 80;
                break;
            case "YOGA":
                minScore = 60; // Need several good taps
                didPass = gameScore >= minScore;
                break;
            case "JUMPROPE":
                minScore = 80; // Need to stay on beat
                didPass = gameScore >= minScore;
                break;
            default:
                minScore = 50;
                didPass = gameScore >= minScore;
        }
        
        final boolean didPassFinal = didPass;
        final int finalScore = gameScore;

        // Send result to backend
        spinner.setVisibility(View.VISIBLE);

        JSONObject body = new JSONObject();
        try {
            body.put("workoutType", currentWorkoutType);
            body.put("score", finalScore);
            body.put("durationMs", 15000);
            body.put("passed", didPass);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiClient.BASE_URL + "/gym/workout?userId=" + userId,
                body,
                response -> {
                    spinner.setVisibility(View.GONE);
                    gameContainerCard.setVisibility(View.GONE);

                    boolean success = response.optBoolean("success", false);
                    double cash = response.optDouble("rewardCash", 0);
                    double xp = response.optDouble("rewardXp", 0);
                    double multiplier = response.optDouble("multiplier", 1);

                    if (success) {
                        Toast.makeText(this, 
                            String.format(Locale.US, "Great workout! +$%.2f (%.0fx multiplier)", cash, multiplier),
                            Toast.LENGTH_LONG).show();
                        
                        // Update local state
                        workoutsToday++;
                        totalWorkouts++;
                        
                        // Refresh UI and HUD
                        updateMembershipUI();
                        HudSyncHelper.refreshHud(this, userId);
                    } else {
                        String msg = response.optString("message", "Workout failed. Try again!");
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    spinner.setVisibility(View.GONE);
                    gameContainerCard.setVisibility(View.GONE);
                    
                    // If backend doesn't support gym yet, show local success
                    if (didPassFinal) {
                        Toast.makeText(this, 
                            String.format(Locale.US, "Workout complete! Score: %d", finalScore),
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, 
                            String.format(Locale.US, "Try again! Score: %d (Need better performance)", finalScore),
                            Toast.LENGTH_SHORT).show();
                    }
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private double getLocalReward(String type) {
        double base;
        switch (type) {
            case "WEIGHTLIFTING": base = 4.0; break;
            case "TREADMILL": base = 3.0; break;
            case "YOGA": base = 3.5; break;
            case "JUMPROPE": base = 2.5; break;
            default: base = 3.0;
        }
        return base * Math.max(1.0, rewardMultiplier);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
