package cycredit.io;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

import cycredit.io.guilds.ApiClient;

// Top-level interface so the activity can implement it
interface ArcadeGameView {
    void startGame(String difficulty);
    void bindListener(GameListener listener);

    interface GameListener {
        void onGameFinished(String gameType, String difficulty, int score, boolean passed, long durationMs);
    }
}

public class CurtissArcadeActivity extends AppCompatActivity implements ArcadeGameView.GameListener {

    private int userId;
    private Spinner difficultySpinner;
    private Spinner gameSpinner;
    private FrameLayout gameContainer;
    private TextView resultBanner;
    private LinearLayout historyContainer;
    private ProgressBar progressBar;
    private TextView payoutText;

    private ArcadeGameView currentGame;
    private String currentDifficulty = "EASY";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curtiss_arcade);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Curtiss Arcade");
        }

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) {
            userId = Session.getUserId(this);
        }
        
        // Defensive check for required extras
        if (userId <= 0) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupSpinners();
        setupBackButton();
        loadConfig();
        loadHistory();
        swapGame(gameSpinner.getSelectedItemPosition());
    }
    
    private void setupBackButton() {
        Button backBtn = findViewById(R.id.btn_back_to_map);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    private void bindViews() {
        difficultySpinner = findViewById(R.id.difficulty_spinner);
        gameSpinner = findViewById(R.id.game_spinner);
        gameContainer = findViewById(R.id.game_container);
        resultBanner = findViewById(R.id.result_banner);
        historyContainer = findViewById(R.id.history_container);
        progressBar = findViewById(R.id.progress_bar);
        payoutText = findViewById(R.id.payout_text);
    }

    private void setupSpinners() {
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Easy", "Medium", "Hard"});
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);
        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        currentDifficulty = "MEDIUM";
                        break;
                    case 2:
                        currentDifficulty = "HARD";
                        break;
                    default:
                        currentDifficulty = "EASY";
                        break;
                }
                if (currentGame != null) currentGame.startGame(currentDifficulty);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Packing Line", "Beat the Hawks", "Memory Tiles"});
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameSpinner.setAdapter(gameAdapter);
        gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                swapGame(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void swapGame(int position) {
        ArcadeGameView view;
        if (position == 1) {
            view = new BeatTheHawksGameView(this);
        } else if (position == 2) {
            view = new MemoryTilesGameView(this);
        } else {
            view = new PackingLineGameView(this);
        }
        view.bindListener(this);
        currentGame = view;
        gameContainer.removeAllViews();
        gameContainer.addView((View) view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        view.startGame(currentDifficulty);
    }

    private void loadConfig() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiClient.BASE_URL + "/job/config",
                null,
                response -> {
                    payoutText.setText(String.format(Locale.US,
                            "Easy $%.0f • Medium $%.0f • Hard $%.0f (Soft cap: -%.0f%% after %d passes/hr)",
                            response.optDouble("easyPayout", 6),
                            response.optDouble("mediumPayout", 10),
                            response.optDouble("hardPayout", 16),
                            response.optDouble("softCapReduction", 0.4) * 100,
                            response.optInt("softCapThreshold", 10)));
                    cycredit.io.util.ToastHelper.showRead(this, "job config", 1);
                },
                error -> {
                    cycredit.io.util.ToastHelper.showError(this, "Fetch job config", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void loadHistory() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                ApiClient.BASE_URL + "/job/history?userId=" + userId + "&limit=6",
                null,
                response -> {
                    renderHistory(response);
                    cycredit.io.util.ToastHelper.showList(this, "job runs", response.length());
                },
                error -> {
                    historyContainer.removeAllViews();
                    cycredit.io.util.ToastHelper.showError(this, "Fetch job history", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void renderHistory(JSONArray array) {
        historyContainer.removeAllViews();
        if (array == null || array.length() == 0) {
            TextView view = new TextView(this);
            view.setText("No runs yet.");
            view.setTextColor(0xFFB0C4D8); // Light gray
            view.setTextSize(14f);
            historyContainer.addView(view);
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            TextView row = new TextView(this);
            boolean passed = obj.optBoolean("passed");
            String text = String.format(Locale.US,
                    "%s (%s) – %s  •  $%.2f",
                    obj.optString("gameType"),
                    obj.optString("difficulty"),
                    passed ? "PASS" : "FAIL",
                    obj.optDouble("rewardCash"));
            row.setText(text);
            row.setTextColor(passed ? 0xFF00FF88 : 0xFFE53935); // Green for pass, red for fail
            row.setTextSize(14f);
            row.setPadding(0, 12, 0, 12);
            historyContainer.addView(row);
        }
    }

    @Override
    public void onGameFinished(String gameType, String difficulty, int score, boolean passed, long durationMs) {
        resultBanner.setBackgroundColor(getResources().getColor(
                passed ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        resultBanner.setText(String.format(Locale.US, "%s %s – %s (score %d)",
                gameType, difficulty, passed ? "PASS" : "FAIL", score));

        progressBar.setVisibility(View.VISIBLE);
        JSONObject body = new JSONObject();
        try {
            body.put("gameType", gameType);
            body.put("difficulty", difficulty);
            body.put("score", score);
            body.put("passed", passed);
            body.put("durationMs", durationMs);
            body.put("runNonce", UUID.randomUUID().toString());
        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiClient.BASE_URL + "/job/run?userId=" + userId,
                body,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    double reward = response.optDouble("rewardCash", 0);
                    if (reward > 0) {
                        String message = String.format(Locale.US, "✅ Job completed! +$%.2f (+%.0f XP)",
                                reward, response.optDouble("rewardXp", reward));
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        cycredit.io.util.ToastHelper.showCreate(this, "job run");
                        animateReward(String.format(Locale.US, "Payout +$%.2f (+%.0f XP)",
                                reward, response.optDouble("rewardXp", reward)));
                        HudSyncHelper.refreshHud(this, userId);
                    } else if (response.optBoolean("softCapApplied", false)) {
                        Toast.makeText(this, "⚠️ Soft cap applied – payout reduced", Toast.LENGTH_SHORT).show();
                        cycredit.io.util.ToastHelper.showInfo(this, "Soft cap applied – payout reduced");
                        animateReward("Soft cap hit – payout reduced.");
                    } else {
                        Toast.makeText(this, "❌ Job failed – no reward", Toast.LENGTH_SHORT).show();
                        cycredit.io.util.ToastHelper.showInfo(this, "Job failed – no reward");
                    }
                    loadHistory();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    cycredit.io.util.ToastHelper.showError(this, "Submit job run", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                    cycredit.io.util.ErrorHandler.handleError(this, error, "Failed to record run");
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void animateReward(String text) {
        resultBanner.setText(text);
        int startColor = getResources().getColor(android.R.color.holo_orange_dark);
        int endColor = getResources().getColor(android.R.color.holo_green_dark);
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        animator.setDuration(600);
        animator.setRepeatCount(1);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(a -> resultBanner.setBackgroundColor((int) a.getAnimatedValue()));
        animator.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ===== Game views =====
    
    // UI Colors for dark theme
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;      // White
    private static final int TEXT_SECONDARY = 0xFFB0C4D8;    // Light blue-gray
    private static final int ACCENT_COLOR = 0xFF00C8FF;      // Cyan
    private static final int SUCCESS_COLOR = 0xFF00FF88;     // Green
    private static final int BG_CARD = 0xFF1A2535;           // Dark card bg

    private static abstract class BaseGameView extends LinearLayout implements ArcadeGameView {
        protected GameListener listener;
        protected long startTime;

        public BaseGameView(CurtissArcadeActivity context) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);
            setPadding(32, 32, 32, 32);
            setBackgroundColor(BG_CARD);
        }

        @Override
        public void bindListener(GameListener listener) {
            this.listener = listener;
        }

        protected void finish(String gameType, String difficulty, int score, boolean passed) {
            if (listener != null) {
                long duration = System.currentTimeMillis() - startTime;
                listener.onGameFinished(gameType, difficulty, score, passed, duration);
            }
        }
        
        // Helper to create styled description text
        protected TextView createDescription(String text) {
            TextView tv = new TextView(getContext());
            tv.setText(text);
            tv.setTextColor(TEXT_SECONDARY);
            tv.setTextSize(14f);
            tv.setPadding(0, 0, 0, 16);
            return tv;
        }
        
        // Helper to create styled counter text
        protected TextView createCounterText() {
            TextView tv = new TextView(getContext());
            tv.setTextColor(TEXT_PRIMARY);
            tv.setTextSize(24f);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setPadding(0, 16, 0, 16);
            tv.setGravity(Gravity.CENTER);
            return tv;
        }
        
        // Helper to create styled action button
        protected Button createActionButton(String text) {
            Button btn = new Button(getContext());
            btn.setText(text);
            btn.setTextColor(TEXT_PRIMARY);
            btn.setTextSize(16f);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            btn.setBackgroundColor(ACCENT_COLOR);
            btn.setPadding(48, 24, 48, 24);
            return btn;
        }
    }

    private static class PackingLineGameView extends BaseGameView {
        private TextView counterText;
        private TextView currentItemText;
        private FrameLayout gameArea;
        private Button buttonA, buttonB, buttonC;
        private CountDownTimer timer;
        private Handler handler = new Handler(Looper.getMainLooper());
        private int target;
        private int correct;
        private int total;
        private String currentItem;
        private String currentCategory;
        private String currentDifficulty;
        private final String[] categories = {"FRAGILE", "HEAVY", "STANDARD"};
        private final String[][] items = {
            {"📦 Glass", "📦 Electronics", "📦 Ceramics"}, // FRAGILE
            {"📦 Books", "📦 Tools", "📦 Metal"}, // HEAVY
            {"📦 Clothes", "📦 Paper", "📦 Plastic"} // STANDARD
        };

        public PackingLineGameView(CurtissArcadeActivity context) {
            super(context);
            
            // Styled description
            addView(createDescription("Sort items into the correct bins! Match the item to its category!"));

            // Styled counter
            counterText = createCounterText();
            addView(counterText);
            
            // Current item display
            currentItemText = new TextView(context);
            currentItemText.setTextColor(TEXT_PRIMARY);
            currentItemText.setTextSize(24f);
            currentItemText.setTypeface(null, android.graphics.Typeface.BOLD);
            currentItemText.setGravity(Gravity.CENTER);
            currentItemText.setText("Waiting for item...");
            LayoutParams itemParams = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            itemParams.setMargins(32, 16, 32, 16);
            currentItemText.setLayoutParams(itemParams);
            addView(currentItemText);
            
            // Game area (for future animations)
            gameArea = new FrameLayout(context);
            gameArea.setLayoutParams(new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            addView(gameArea);

            // Three sorting buttons
            LinearLayout buttonRow = new LinearLayout(context);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setGravity(Gravity.CENTER);
            buttonRow.setLayoutParams(new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            
            // Create buttons with consistent sizing
            int buttonHeight = (int)(48 * getContext().getResources().getDisplayMetrics().density); // 48dp in pixels
            
            buttonA = createActionButton("FRAGILE");
            LinearLayout.LayoutParams paramsA = new LinearLayout.LayoutParams(0, buttonHeight, 1f);
            paramsA.setMargins(4, 0, 4, 0);
            buttonA.setLayoutParams(paramsA);
            buttonA.setBackgroundColor(0xFF64B5F6); // Blue
            buttonA.setGravity(Gravity.CENTER);
            buttonA.setPadding(4, 8, 4, 8);
            buttonA.setTextSize(12f); // Smaller text to fit timer
            buttonA.setSingleLine(true);
            buttonA.setEllipsize(android.text.TextUtils.TruncateAt.END);
            buttonA.setOnClickListener(v -> sortItem(0));
            
            buttonB = createActionButton("HEAVY");
            LinearLayout.LayoutParams paramsB = new LinearLayout.LayoutParams(0, buttonHeight, 1f);
            paramsB.setMargins(4, 0, 4, 0);
            buttonB.setLayoutParams(paramsB);
            buttonB.setBackgroundColor(0xFFE57373); // Red
            buttonB.setGravity(Gravity.CENTER);
            buttonB.setPadding(4, 8, 4, 8);
            buttonB.setTextSize(12f);
            buttonB.setSingleLine(true);
            buttonB.setEllipsize(android.text.TextUtils.TruncateAt.END);
            buttonB.setOnClickListener(v -> sortItem(1));
            
            buttonC = createActionButton("STANDARD");
            LinearLayout.LayoutParams paramsC = new LinearLayout.LayoutParams(0, buttonHeight, 1f);
            paramsC.setMargins(4, 0, 4, 0);
            buttonC.setLayoutParams(paramsC);
            buttonC.setBackgroundColor(0xFF81C784); // Green
            buttonC.setGravity(Gravity.CENTER);
            buttonC.setPadding(4, 8, 4, 8);
            buttonC.setTextSize(12f); // Same size as others
            buttonC.setSingleLine(true);
            buttonC.setEllipsize(android.text.TextUtils.TruncateAt.END);
            buttonC.setOnClickListener(v -> sortItem(2));
            
            buttonRow.addView(buttonA);
            buttonRow.addView(buttonB);
            buttonRow.addView(buttonC);
            addView(buttonRow);
        }
        
        private void sortItem(int selectedCategory) {
            if (currentItem == null) return;
            
            total++;
            boolean isCorrect = (selectedCategory == getCategoryIndex(currentCategory));
            
            if (isCorrect) {
                correct++;
                currentItemText.setTextColor(SUCCESS_COLOR);
                currentItemText.setText("✓ Correct! " + currentItem);
            } else {
                currentItemText.setTextColor(0xFFE53935);
                currentItemText.setText("✗ Wrong! " + currentItem + " → " + categories[selectedCategory]);
            }
            
            counterText.setText(String.format(Locale.US, "Correct: %d / %d", correct, total));
            
            if (correct >= target) {
                timer.cancel();
                handler.removeCallbacksAndMessages(null);
                counterText.setTextColor(SUCCESS_COLOR);
                finish("PackingLine", currentDifficulty(), correct, true);
            } else {
                // Schedule next item
                handler.postDelayed(this::spawnNextItem, 1500);
            }
        }
        
        private int getCategoryIndex(String category) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(category)) return i;
            }
            return 0;
        }
        
        private void spawnNextItem() {
            // Randomly select a category and item
            int categoryIndex = (int)(Math.random() * categories.length);
            currentCategory = categories[categoryIndex];
            String[] categoryItems = items[categoryIndex];
            currentItem = categoryItems[(int)(Math.random() * categoryItems.length)];
            
            currentItemText.setTextColor(TEXT_PRIMARY);
            currentItemText.setText(currentItem);
        }

        @Override
        public void startGame(String difficulty) {
            currentDifficulty = difficulty;
            correct = 0;
            total = 0;
            currentItem = null;
            counterText.setTextColor(TEXT_PRIMARY);
            currentItemText.setTextColor(TEXT_PRIMARY);
            handler.removeCallbacksAndMessages(null);

            String diffUpper = difficulty.toUpperCase(Locale.US);
            switch (diffUpper) {
                case "MEDIUM":
                    target = 12;
                    break;
                case "HARD":
                    target = 16;
                    break;
                default:
                    target = 10;
                    break;
            }

            long timeMs;
            switch (diffUpper) {
                case "MEDIUM":
                    timeMs = 30000L;
                    break;
                case "HARD":
                    timeMs = 25000L;
                    break;
                default:
                    timeMs = 35000L;
                    break;
            }

            counterText.setText(String.format(Locale.US, "Correct: 0 / %d", target));
            startTime = System.currentTimeMillis();
            if (timer != null) timer.cancel();
            timer = new CountDownTimer(timeMs, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Update button text with timer
                    long seconds = millisUntilFinished / 1000;
                    buttonA.setText("FRAGILE (" + seconds + "s)");
                    buttonB.setText("HEAVY (" + seconds + "s)");
                    buttonC.setText("STANDARD (" + seconds + "s)");
                }

                @Override
                public void onFinish() {
                    handler.removeCallbacksAndMessages(null);
                    boolean passed = correct >= target;
                    counterText.setTextColor(passed ? SUCCESS_COLOR : 0xFFE53935);
                    finish("PackingLine", currentDifficulty(), correct, passed);
                }
            };
            timer.start();
            // Spawn first item after a short delay
            handler.postDelayed(this::spawnNextItem, 1000);
        }

        private String currentDifficulty() {
            return currentDifficulty;
        }
    }

    private static class BeatTheHawksGameView extends BaseGameView {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private FrameLayout hawksField;
        private TextView scoreText;
        private int target;
        private int defeated;
        private CountDownTimer timer;
        private final List<View> activeHawks = new ArrayList<>();

        public BeatTheHawksGameView(CurtissArcadeActivity context) {
            super(context);
            
            // Styled description
            addView(createDescription("Defeat the Hawkeye mascots that pop up! Tap them before they disappear!"));

            // Score display
            scoreText = createCounterText();
            scoreText.setText("Defeated: 0");
            addView(scoreText);

            hawksField = new FrameLayout(context);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            hawksField.setLayoutParams(params);
            hawksField.setBackgroundColor(0xFF1B263B);
            addView(hawksField);
        }

        @Override
        public void startGame(String difficulty) {
            defeated = 0;
            hawksField.removeAllViews();
            activeHawks.clear();
            scoreText.setTextColor(TEXT_PRIMARY);

            String diffUpper = difficulty.toUpperCase(Locale.US);

            switch (diffUpper) {
                case "MEDIUM":
                    target = 12;
                    break;
                case "HARD":
                    target = 16;
                    break;
                default:
                    target = 8;
                    break;
            }
            
            scoreText.setText(String.format(Locale.US, "Defeated: 0 / %d", target));

            int spawnInterval;
            switch (diffUpper) {
                case "MEDIUM":
                    spawnInterval = 900;
                    break;
                case "HARD":
                    spawnInterval = 700;
                    break;
                default:
                    spawnInterval = 1100;
                    break;
            }

            long durationMs = 25000L;
            startTime = System.currentTimeMillis();
            if (timer != null) timer.cancel();
            timer = new CountDownTimer(durationMs, 1000) {
                @Override
                public void onTick(long millisUntilFinished) { }

                @Override
                public void onFinish() {
                    handler.removeCallbacksAndMessages(null);
                    finish("BeatTheHawks", difficulty, defeated, defeated >= target);
                }
            };
            timer.start();
            scheduleNextHawk(spawnInterval, difficulty);
        }

        private void scheduleNextHawk(int interval, String difficulty) {
            handler.postDelayed(() -> spawnHawk(interval, difficulty), interval);
        }

        private void spawnHawk(int interval, String difficulty) {
            // Get field dimensions for random positioning
            hawksField.post(() -> {
                int fieldWidth = hawksField.getWidth();
                int fieldHeight = hawksField.getHeight();
                
                if (fieldWidth == 0 || fieldHeight == 0) {
                    // Fallback if not measured yet
                    fieldWidth = 800;
                    fieldHeight = 600;
                }
                
                // Random position (leave margins for button size)
                int buttonSize = 120;
                int maxX = Math.max(buttonSize, fieldWidth - buttonSize);
                int maxY = Math.max(buttonSize, fieldHeight - buttonSize);
                int x = (int)(Math.random() * maxX);
                int y = (int)(Math.random() * maxY);
                
                // Create clickable view with hawkeye image or emoji
                final View hawk;
                int resId = 0;
                try {
                    resId = getContext().getResources()
                            .getIdentifier("ic_hawk", "drawable", getContext().getPackageName());
                } catch (Exception e) {
                    // Resource not found, will use fallback
                }
                
                if (resId != 0) {
                    // Use ImageButton for the image
                    android.widget.ImageButton imageButton = new android.widget.ImageButton(getContext());
                    imageButton.setImageResource(resId);
                    imageButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                    imageButton.setBackgroundColor(0x00000000); // Transparent
                    imageButton.setPadding(8, 8, 8, 8);
                    hawk = imageButton;
                } else {
                    // Fallback to Button with emoji
                    Button hawkButton = new Button(getContext());
                    hawkButton.setText("🦅");
                    hawkButton.setTextColor(TEXT_PRIMARY);
                    hawkButton.setTextSize(48f);
                    hawkButton.setTypeface(null, android.graphics.Typeface.BOLD);
                    hawkButton.setBackgroundColor(0xFF000000);
                    hawkButton.setPadding(16, 16, 16, 16);
                    hawk = hawkButton;
                }
                
                // Position absolutely
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        buttonSize, buttonSize);
                params.leftMargin = x;
                params.topMargin = y;
                hawk.setLayoutParams(params);
                
                hawk.setOnClickListener(v -> {
                    defeated++;
                    scoreText.setText(String.format(Locale.US, "Defeated: %d / %d", defeated, target));
                    hawksField.removeView(hawk);
                    activeHawks.remove(hawk);
                    
                    // Visual feedback
                    v.animate()
                            .scaleX(1.5f)
                            .scaleY(1.5f)
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction(() -> {
                                if (defeated >= target) {
                                    if (timer != null) timer.cancel();
                                    handler.removeCallbacksAndMessages(null);
                                    scoreText.setTextColor(SUCCESS_COLOR);
                                    finish("BeatTheHawks", difficulty, defeated, true);
                                }
                            })
                            .start();
                });
                
                // Auto-remove after 3 seconds if not clicked
                handler.postDelayed(() -> {
                    if (hawksField.indexOfChild(hawk) >= 0) {
                        hawksField.removeView(hawk);
                        activeHawks.remove(hawk);
                    }
                }, 3000);
                
                hawksField.addView(hawk);
                activeHawks.add(hawk);
                
                // Schedule next hawk
                scheduleNextHawk(interval, difficulty);
            });
        }
    }

    private static class MemoryTilesGameView extends BaseGameView {
        private final int[] COLORS = {
                0xFFE57373, 0xFF64B5F6, 0xFF81C784, 0xFFFFB74D
        };
        private final Queue<Integer> pattern = new ArrayDeque<>();
        private final List<Button> buttons = new ArrayList<>();
        private TextView statusText;
        private boolean acceptingInput = false;
        private int expectedIndex = 0;
        private int patternLength;
        private String currentDifficulty = "EASY";

        public MemoryTilesGameView(CurtissArcadeActivity context) {
            super(context);
            
            // Styled description
            addView(createDescription("Memorize the light pattern and repeat it."));
            
            // Status text
            statusText = createCounterText();
            statusText.setText("Watch the pattern...");
            addView(statusText);

            LinearLayout grid = new LinearLayout(context);
            grid.setOrientation(LinearLayout.VERTICAL);
            grid.setGravity(Gravity.CENTER);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            grid.setLayoutParams(params);
            grid.setPadding(16, 16, 16, 16);
            addView(grid);

            for (int r = 0; r < 2; r++) {
                LinearLayout row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER);
                row.setPadding(8, 8, 8, 8);
                for (int c = 0; c < 2; c++) {
                    Button btn = new Button(context);
                    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                            0, 150, 1f);
                    btnParams.setMargins(8, 8, 8, 8);
                    btn.setLayoutParams(btnParams);
                    btn.setText("");
                    btn.setBackgroundColor(COLORS[r * 2 + c]);
                    int index = r * 2 + c;
                    btn.setOnClickListener(v -> handleInput(index));
                    row.addView(btn);
                    buttons.add(btn);
                }
                grid.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }

        @Override
        public void startGame(String difficulty) {
            currentDifficulty = difficulty;
            pattern.clear();
            acceptingInput = false;
            expectedIndex = 0;

            String diffUpper = difficulty.toUpperCase(Locale.US);
            switch (diffUpper) {
                case "MEDIUM":
                    patternLength = 5;
                    break;
                case "HARD":
                    patternLength = 6;
                    break;
                default:
                    patternLength = 4;
                    break;
            }

            for (int i = 0; i < patternLength; i++) {
                pattern.add((int) (Math.random() * COLORS.length));
            }
            startTime = System.currentTimeMillis();
            playPattern();
        }

        private void playPattern() {
            acceptingInput = false;
            expectedIndex = 0;
            statusText.setText("Watch the pattern...");
            statusText.setTextColor(TEXT_SECONDARY);
            List<Integer> snapshot = new ArrayList<>(pattern);
            Handler handler = new Handler(Looper.getMainLooper());
            for (int i = 0; i < snapshot.size(); i++) {
                int colorIndex = snapshot.get(i);
                final int delayIndex = i;
                handler.postDelayed(() -> highlightButton(colorIndex),
                        delayIndex * 700L);
            }
            handler.postDelayed(() -> {
                acceptingInput = true;
                statusText.setText("Your turn! Repeat the pattern");
                statusText.setTextColor(ACCENT_COLOR);
            }, snapshot.size() * 700L + 200);
        }

        private void highlightButton(int index) {
            Button button = buttons.get(index);
            int original = COLORS[index];
            ValueAnimator animator = ValueAnimator.ofObject(
                    new ArgbEvaluator(), original, 0xFFFFFFFF, original);
            animator.setDuration(600);
            animator.addUpdateListener(a ->
                    button.setBackgroundColor((int) a.getAnimatedValue()));
            animator.start();
        }

        private void handleInput(int index) {
            if (!acceptingInput) return;
            List<Integer> snapshot = new ArrayList<>(pattern);
            int expectedColorIndex = snapshot.get(expectedIndex);
            if (index == expectedColorIndex) {
                expectedIndex++;
                statusText.setText(String.format(Locale.US, "Correct! %d / %d", expectedIndex, patternLength));
                if (expectedIndex >= patternLength) {
                    statusText.setTextColor(SUCCESS_COLOR);
                    statusText.setText("Perfect! Pattern complete!");
                    finish("MemoryTiles", currentDifficulty, patternLength, true);
                }
            } else {
                statusText.setTextColor(0xFFE53935); // Red
                statusText.setText("Wrong! Game over.");
                finish("MemoryTiles", currentDifficulty, expectedIndex, false);
            }
        }
    }
}
