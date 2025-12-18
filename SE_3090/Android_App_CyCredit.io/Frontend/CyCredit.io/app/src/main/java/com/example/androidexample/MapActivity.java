package cycredit.io;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.util.Log;

import cycredit.io.guilds.ApiClient;

public class MapActivity extends AppCompatActivity {

    private FrameLayout overlay;
    private TextView hudTurns, hudCash, hudScore;
    private BroadcastReceiver hudReceiver;

    private int userId;
    private String email;
    private String username;
    private final List<MapLocationModel> pois = new ArrayList<>();
    
    // Navigation state
    private boolean isNavigating = false; // Prevent double navigation
    private Vibrator vibrator;
    
    // Timing constants
    private static final int HAPTIC_CONFIRM = 20; // Stronger tick for navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        userId = getIntent().getIntExtra("USER_ID", -1);
        // Fallback to prefs if intent extra is missing
        if (userId <= 0) {
            userId = cycredit.io.util.UserPrefs.userId(this);
        }
        // Also try Session as additional fallback
        if (userId <= 0) {
            userId = Session.getUserId(this);
        }
        email  = getIntent().getStringExtra("EMAIL");
        username = getIntent().getStringExtra("USERNAME");
        if (username == null) username = Session.getUsername(this);
        
        // Defensive check for required extras
        if (userId <= 0 || email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Mark Welcome Aboard as completed once and award leaderboard points
        completeWelcomeQuest();

        overlay  = findViewById(R.id.overlay_container);
        hudTurns = findViewById(R.id.hud_turns);
        hudCash  = findViewById(R.id.hud_cash);
        hudScore = findViewById(R.id.hud_score);
        
        // Initialize vibrator for haptics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        
        // No longer need outside tap detection for single-click

        // Bottom Navigation (PS-style, like PlayStation app)
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.action_bar);
        if (bottomNav != null) {
            setupBottomNavigation(bottomNav, R.id.nav_map);
        }

        // Header buttons
        
        // Public Chat button
        ImageButton btnPublicChat = findViewById(R.id.btn_public_chat);
        if (btnPublicChat != null) {
            btnPublicChat.setOnClickListener(v -> {
                // Navigate to public chat
                Intent i = new Intent(this, cycredit.io.chat.MessageActivity.class);
                i.putExtra(cycredit.io.chat.MessageActivity.EXTRA_SCOPE, "public");
                i.putExtra(cycredit.io.chat.MessageActivity.EXTRA_CHANNEL, "global");
                i.putExtra(cycredit.io.chat.MessageActivity.EXTRA_USER_ID, userId);
                i.putExtra(cycredit.io.chat.MessageActivity.EXTRA_UNAME, email);
                startActivity(i);
            });
        }
        
        // Notifications button
        ImageButton btnNotifications = findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                // Navigate to Join Guild Activity to view guild invites (notifications)
                Intent i = new Intent(this, JoinGuildActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                startActivity(i);
            });
        }

        ImageButton btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                // Show settings dialog with options
                showSettingsDialog();
            });
        }

        // Update header username - use "User" or from Session
        TextView headerUsername = findViewById(R.id.header_username);
        if (headerUsername != null) {
            if (username != null && !username.isEmpty()) {
                headerUsername.setText(username);
            } else {
                // Fallback to "User" if no username in session
                headerUsername.setText("User");
            }
        }

        // Update header time (optional - can be updated periodically)
        TextView headerTime = findViewById(R.id.header_time);
        if (headerTime != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault());
            headerTime.setText(sdf.format(new java.util.Date()));
        }

        // (Optional) clear local quest flags during testing
        getSharedPreferences("quest_explorer_flags", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("quest_welcome_flags",  MODE_PRIVATE).edit().clear().apply();

        fetchResource();
        fetchLocationsOrSeed();
        fetchUserAvatar(); // Load user's selected avatar
        
        // End month button (if exists in layout)
        View endMonthBtn = findViewById(R.id.btn_end_month);
        if (endMonthBtn != null) {
            endMonthBtn.setOnClickListener(v -> endMonth());
        }

        hudReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int turns = intent.getIntExtra("turnsLeft", 0);
                double money = intent.getDoubleExtra("money", 0);
                double credit = intent.getDoubleExtra("credit", 0);
                hudTurns.setText("Turns: " + turns);
                hudCash.setText("Cash: $" + String.format("%.2f", money));
                hudScore.setText("Score: " + (int) credit);
            }
        };
        registerReceiver(hudReceiver, new IntentFilter(HudSyncHelper.ACTION_HUD_UPDATE));
    }

    private void setupBottomNavigation(com.google.android.material.bottomnavigation.BottomNavigationView bottomNav, int selectedItemId) {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_map) {
                // Already on Map - do nothing
                return true;
            } else if (itemId == R.id.nav_guilds) {
                int uid = (userId > 0) ? userId : Session.getUserId(this);
                String uname = Session.getUsername(this);
                if (uname == null) uname = username; // Use username from onCreate if available
                Intent i1 = new Intent(this, cycredit.io.guilds.GuildsActivity.class);
                i1.putExtra("USER_ID", uid);
                i1.putExtra("EMAIL", email);
                i1.putExtra("USERNAME", uname);
                i1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i1);
                finish();
                return true;
            } else if (itemId == R.id.nav_missions) {
                Intent i = new Intent(this, MissionsActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                Intent i = new Intent(this, cycredit.io.LeaderboardActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            } else if (itemId == R.id.nav_end_turn) {
                // End turn functionality - refresh resource (only on Map screen)
                fetchResource();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(selectedItemId);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hudReceiver != null) {
            unregisterReceiver(hudReceiver);
            hudReceiver = null;
        }
        isNavigating = false;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure clean state when returning
        isNavigating = false;
        // Always refresh HUD when returning to map (authoritative source of truth)
        fetchResource();
    }

    private void fetchResource() {
        String url = ApiClient.BASE_URL + "/game/state?userId=" + userId;
        Log.d("MapActivity", "=== FETCHING GAME STATE ===");
        Log.d("MapActivity", "URL: " + url);
        
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("MapActivity", "Game state response: " + response.toString());
                        
                        int turns = response.optInt("turnsLeft", 5);
                        double money = response.optDouble("money", 0);
                        double credit = response.optDouble("creditScore", 700);
                        int month = response.optInt("currentMonth", 1);
                        
                        Log.d("MapActivity", "Parsed: turns=" + turns + ", money=" + money + 
                            ", credit=" + credit + ", month=" + month);
                        
                        // Initialize month tracking for transaction filtering
                        initMonthTrackingIfNeeded(month);
                        
                        hudTurns.setText("Turns: " + turns);
                        hudCash.setText("Cash: $" + String.format("%.2f", money));
                        hudScore.setText("Score: " + (int) credit);
                        
                        // Show end month button if turns are 0
                        View endMonthBtn = findViewById(R.id.btn_end_month);
                        if (endMonthBtn != null) {
                            endMonthBtn.setVisibility(turns == 0 ? View.VISIBLE : View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e("MapActivity", "Error parsing game state", e);
                    }
                },
                err -> {
                    Log.w("MapActivity", "Game state failed, trying fallback: " + err.getMessage());
                    // Fallback to old endpoint
                    String fallbackUrl = ApiClient.BASE_URL + "/resource/" + userId;
                    StringRequest fallback = new StringRequest(
                            Request.Method.GET, fallbackUrl,
                            resp -> {
                                try {
                                    Log.d("MapActivity", "Fallback response: " + resp);
                                    JSONObject json = new JSONObject(resp);
                                    int turns = json.optInt("turnsLeft", 5);
                                    double money = json.optDouble("money", 0);
                                    double credit = json.optDouble("credit", 700);
                                    
                                    Log.d("MapActivity", "Fallback parsed: turns=" + turns + 
                                        ", money=" + money + ", credit=" + credit);
                                    
                                    hudTurns.setText("Turns: " + turns);
                                    hudCash.setText("Cash: $" + String.format("%.2f", money));
                                    hudScore.setText("Score: " + (int) credit);
                                } catch (Exception e) {
                                    Log.e("MapActivity", "Error parsing fallback", e);
                                }
                            },
                            error -> {
                                Log.e("MapActivity", "Both endpoints failed");
                            }
                    );
                    ApiClient.getRequestQueue(this).add(fallback);
                }
        );
        ApiClient.getRequestQueue(this).add(req);
    }
    
    private static final String PREFS_MONTH_TRACKING = "month_tracking";
    private static final String KEY_MONTH_START_TIME = "month_start_time_";
    private static final String KEY_CURRENT_MONTH = "current_month_";
    
    /**
     * Get the start time of the current month from SharedPreferences.
     * If not set, returns 0 (meaning all transactions will be included).
     */
    private long getMonthStartTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_MONTH_TRACKING, MODE_PRIVATE);
        return prefs.getLong(KEY_MONTH_START_TIME + userId, 0);
    }
    
    /**
     * Set the start time of the current month.
     * Called when a new month begins.
     */
    private void setMonthStartTime(long timeMillis) {
        SharedPreferences prefs = getSharedPreferences(PREFS_MONTH_TRACKING, MODE_PRIVATE);
        prefs.edit().putLong(KEY_MONTH_START_TIME + userId, timeMillis).apply();
        Log.d("MapActivity", "Month start time set to: " + timeMillis + " (" + new Date(timeMillis) + ")");
    }
    
    /**
     * Initialize month tracking if this is the first time or a new month.
     */
    private void initMonthTrackingIfNeeded(int currentMonth) {
        SharedPreferences prefs = getSharedPreferences(PREFS_MONTH_TRACKING, MODE_PRIVATE);
        int storedMonth = prefs.getInt(KEY_CURRENT_MONTH + userId, -1);
        
        if (storedMonth != currentMonth) {
            // New month started - record the start time
            long now = System.currentTimeMillis();
            prefs.edit()
                .putInt(KEY_CURRENT_MONTH + userId, currentMonth)
                .putLong(KEY_MONTH_START_TIME + userId, now)
                .apply();
            Log.d("MapActivity", "New month " + currentMonth + " started at " + new Date(now));
        }
    }
    
    private void endMonth() {
        Log.d("MapActivity", "=== ENDING MONTH ===");
        
        // Step 1: Fetch fresh transactions to compute THIS month's totals
        String txUrl = ApiClient.BASE_URL + "/billing/transactions?userId=" + userId;
        Log.d("MapActivity", "Fetching transactions from: " + txUrl);
        
        JsonArrayRequest txReq = new JsonArrayRequest(
                Request.Method.GET, txUrl, null,
                transactions -> {
                    Log.d("MapActivity", "=== TRANSACTIONS RESPONSE ===");
                    Log.d("MapActivity", "Total transactions fetched: " + transactions.length());
                    
                    // Get month start time for filtering
                    long monthStartTime = getMonthStartTime();
                    Log.d("MapActivity", "Month start time: " + monthStartTime + " (" + 
                        (monthStartTime > 0 ? new Date(monthStartTime).toString() : "not set - using all") + ")");
                    
                    // Compute totals from filtered transactions
                    double[] totals = computeMonthlyTotals(transactions, monthStartTime);
                    double totalSpent = totals[0];
                    double totalPaid = totals[1];
                    int txCount = (int) totals[2];
                    
                    Log.d("MapActivity", "=== COMPUTED MONTHLY TOTALS ===");
                    Log.d("MapActivity", "  Transactions this month: " + txCount);
                    Log.d("MapActivity", "  Total Spent: $" + totalSpent);
                    Log.d("MapActivity", "  Total Paid: $" + totalPaid);
                    
                    // Step 2: Call end-month API
                    callEndMonthApi(totalSpent, totalPaid, txCount);
                },
                error -> {
                    Log.w("MapActivity", "Transactions fetch failed: " + error.getMessage());
                    // Still try to end month even if transactions fail
                    callEndMonthApi(0, 0, 0);
                }
        );
        ApiClient.getRequestQueue(this).add(txReq);
    }
    
    /**
     * Compute monthly totals from transactions, filtering by month start time.
     * @return [totalSpent, totalPaid, transactionCount]
     */
    private double[] computeMonthlyTotals(JSONArray transactions, long monthStartTime) {
        double totalSpent = 0;
        double totalPaid = 0;
        int count = 0;
        
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        for (int i = 0; i < transactions.length(); i++) {
            try {
                JSONObject tx = transactions.getJSONObject(i);
                String timestamp = tx.optString("timestamp", tx.optString("createdAt", ""));
                double amount = Math.abs(tx.optDouble("amount", 0));
                String type = tx.optString("type", "PURCHASE");
                
                // Parse timestamp and filter by month
                long txTime = 0;
                if (!timestamp.isEmpty()) {
                    try {
                        // Handle various timestamp formats
                        String cleanTs = timestamp;
                        if (cleanTs.contains(".")) {
                            cleanTs = cleanTs.substring(0, cleanTs.indexOf('.'));
                        }
                        if (cleanTs.endsWith("Z")) {
                            cleanTs = cleanTs.substring(0, cleanTs.length() - 1);
                        }
                        Date date = isoFormat.parse(cleanTs);
                        if (date != null) {
                            txTime = date.getTime();
                        }
                    } catch (Exception e) {
                        Log.w("MapActivity", "Failed to parse timestamp: " + timestamp);
                    }
                }
                
                // Filter: only include transactions from this month
                // If monthStartTime is 0 (not set), include recent transactions (last 7 days as fallback)
                boolean includeTransaction = false;
                if (monthStartTime > 0) {
                    includeTransaction = txTime >= monthStartTime;
                } else {
                    // Fallback: include transactions from last 7 days
                    long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                    includeTransaction = txTime >= sevenDaysAgo || txTime == 0;
                }
                
                if (includeTransaction) {
                    count++;
                    // Categorize by type
                    if ("PURCHASE".equals(type) || "FEE".equals(type) || "INTEREST".equals(type)) {
                        totalSpent += amount;
                        Log.d("MapActivity", "  [SPENT] " + tx.optString("merchant", "?") + 
                            ": $" + amount + " (" + type + ")");
                    } else if ("PAYMENT".equals(type)) {
                        totalPaid += amount;
                        Log.d("MapActivity", "  [PAID] " + tx.optString("merchant", "?") + 
                            ": $" + amount + " (" + type + ")");
                    }
                } else {
                    Log.d("MapActivity", "  [EXCLUDED - old] " + tx.optString("merchant", "?") + 
                        " at " + timestamp);
                }
            } catch (Exception e) {
                Log.w("MapActivity", "Error processing transaction " + i, e);
            }
        }
        
        return new double[]{totalSpent, totalPaid, count};
    }
    
    /**
     * Actually call the end-month API and show summary with computed totals
     */
    private void callEndMonthApi(double totalSpent, double totalPaid, int txCount) {
        String url = ApiClient.BASE_URL + "/game/end-month?userId=" + userId;
        Log.d("MapActivity", "Calling end-month: " + url);
        
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, null,
                response -> {
                    Log.d("MapActivity", "=== END MONTH RESPONSE ===");
                    try {
                        Log.d("MapActivity", response.toString(2)); // Pretty print
                    } catch (Exception e) {
                        Log.d("MapActivity", response.toString());
                    }
                    
                    // Log statement generation info
                    Log.d("MapActivity", "=== STATEMENT ROLLOVER ===");
                    Log.d("MapActivity", "  Monthly spend tracked: $" + totalSpent);
                    Log.d("MapActivity", "  Payments made: $" + totalPaid);
                    
                    // Calculate statement due amount
                    double statementDue = Math.max(0, totalSpent - totalPaid);
                    
                    // Calculate minimum due: max($25 floor, 10% of total), but NEVER exceed total
                    // This matches backend logic in GameService.generateStatement()
                    double minimumDue;
                    if (statementDue <= 0) {
                        minimumDue = 0;
                    } else {
                        double computedMin = Math.max(25.0, statementDue * 0.10);
                        minimumDue = Math.min(statementDue, computedMin);
                    }
                    
                    Log.d("MapActivity", "  Calculated statement due: $" + statementDue);
                    Log.d("MapActivity", "  Calculated minimum due: $" + minimumDue);
                    
                    // === SAVE PENDING STATEMENT FOR FINANCEHUB ===
                    // This ensures FinanceHub can display the correct due even if
                    // backend statement generation has timing issues
                    if (statementDue > 0) {
                        int currentMonth = response.optInt("newMonth", 
                            response.optInt("currentMonth", 1));
                        FinanceHubActivity.savePendingStatement(this, userId, 
                            statementDue, minimumDue, currentMonth);
                        Log.d("MapActivity", "Saved pending statement for FinanceHub");
                    }
                    
                    // After month ends, reset month start time for next month
                    setMonthStartTime(System.currentTimeMillis());
                    
                    // Show monthly summary with computed totals
                    showMonthlySummary(response, totalSpent, totalPaid, txCount);
                    
                    // Refresh game state and trigger HUD sync for other activities
                    fetchResource();
                    HudSyncHelper.refreshHud(this, userId);
                    
                    // Toast to inform user about statement
                    if (statementDue > 0) {
                        Toast.makeText(this, 
                            String.format("Statement generated! $%.2f due. Visit Beardshear to pay.", statementDue), 
                            Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e("MapActivity", "End month failed: " + error.getMessage());
                    cycredit.io.util.ErrorHandler.handleError(this, error, "Failed to end month");
                }
        );
        ApiClient.getRequestQueue(this).add(req);
    }
    
    /**
     * Show Monthly Summary dialog with computed totals from filtered transactions
     */
    private void showMonthlySummary(JSONObject summary, double totalSpent, double totalPaid, int txCount) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Monthly Summary");
        
        StringBuilder message = new StringBuilder();
        try {
            // Log all fields for debugging
            Log.d("MapActivity", "=== PARSING MONTHLY SUMMARY ===");
            java.util.Iterator<String> keys = summary.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Log.d("MapActivity", "  " + key + " = " + summary.opt(key));
            }
            
            double oldScore = summary.optDouble("oldCreditScore", 700);
            double newScore = summary.optDouble("newCreditScore", 700);
            double rawDelta = summary.optDouble("creditScoreDelta", 0);
            
            Log.d("MapActivity", "=== FINAL SUMMARY VALUES ===");
            Log.d("MapActivity", "  Transactions this month: " + txCount);
            Log.d("MapActivity", "  Total Spent: $" + totalSpent);
            Log.d("MapActivity", "  Total Paid: $" + totalPaid);
            Log.d("MapActivity", "  oldScore: " + oldScore + ", newScore: " + newScore + ", rawDelta: " + rawDelta);
            
            // === CREDIT SCORE DELTA SANITY CHECKS ===
            double delta = rawDelta;
            
            // Cap delta to reasonable range (-50 to +50) for demo stability
            final double MAX_DELTA = 50.0;
            if (Math.abs(delta) > MAX_DELTA) {
                Log.w("MapActivity", "CAPPING credit delta from " + delta + " to +/-" + MAX_DELTA);
                delta = Math.signum(delta) * MAX_DELTA;
            }
            
            // If no activity (spent=0 AND paid=0), delta should be near 0
            if (totalSpent == 0 && totalPaid == 0 && Math.abs(delta) > 5) {
                Log.w("MapActivity", "NO ACTIVITY but delta=" + delta + ". Capping to small value.");
                delta = Math.signum(delta) * 5; // Allow small drift for demo
            }
            
            // Recalculate newScore based on capped delta if needed
            if (delta != rawDelta) {
                newScore = oldScore + delta;
                Log.d("MapActivity", "Adjusted newScore to " + newScore + " (delta capped to " + delta + ")");
            }
            
            message.append("Credit Score: ").append((int)oldScore)
                    .append(" → ").append((int)newScore);
            if (delta != 0) {
                message.append(" (").append(delta > 0 ? "+" : "").append(String.format("%.0f", delta)).append(")\n\n");
            } else {
                message.append("\n\n");
            }
            
            message.append("Total Spent: $").append(String.format("%.2f", totalSpent)).append("\n");
            message.append("Total Paid: $").append(String.format("%.2f", totalPaid)).append("\n");
            message.append("Transactions: ").append(txCount).append("\n\n");
            
            // Add explanation if no activity
            if (totalSpent == 0 && totalPaid == 0) {
                message.append("No transactions this month.\n\n");
            }
            
            JSONArray tips = summary.optJSONArray("tips");
            if (tips != null && tips.length() > 0) {
                message.append("Tips:\n");
                for (int i = 0; i < tips.length(); i++) {
                    message.append("• ").append(tips.getString(i)).append("\n");
                }
            }
        } catch (Exception e) {
            Log.e("MapActivity", "Error parsing monthly summary", e);
            message.append("Month ended successfully!");
        }
        
        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void fetchLocationsOrSeed() {
        String url = ApiClient.BASE_URL + "/locations";
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                this::bindLocationsFromServer,
                error -> {
                    pois.clear();
                    pois.add(new MapLocationModel(1, "Beardshear Hall", "FINANCE", 0.25f, 0.35f, getBuildingIcon("Beardshear")));
                    pois.add(new MapLocationModel(2, "Memorial Union", "STORE", 0.62f, 0.40f, getBuildingIcon("Memorial Union")));
                    pois.add(new MapLocationModel(3, "Freddy Court", "HOUSING", 0.18f, 0.70f, getBuildingIcon("Freddy")));
                    pois.add(new MapLocationModel(4, "Curtiss Hall", "JOB", 0.52f, 0.22f, getBuildingIcon("Curtiss")));
                    pois.add(new MapLocationModel(5, "Parks Library", "LIBRARY", 0.80f, 0.28f, getBuildingIcon("Parks")));
                    pois.add(new MapLocationModel(6, "State Gym", "GYM", 0.78f, 0.75f, getBuildingIcon("State Gym")));
                    drawPois();
                }
        );
        ApiClient.getRequestQueue(this).add(req);
    }

    private float normPercent(double v) {
        if (Double.isNaN(v)) return 0.5f;
        if (v > 1.0) v = v / 100.0;
        if (v < 0.0) v = 0.0;
        if (v > 1.0) v = 1.0;
        return (float) v;
    }

    private void bindLocationsFromServer(JSONArray arr) {
        try {
            pois.clear();

            if (arr == null || arr.length() == 0) {
                pois.add(new MapLocationModel(1, "Beardshear Hall", "FINANCE", 0.25f, 0.35f, getBuildingIcon("Beardshear")));
                pois.add(new MapLocationModel(2, "Memorial Union", "STORE", 0.62f, 0.40f, getBuildingIcon("Memorial Union")));
                pois.add(new MapLocationModel(3, "Freddy Court", "HOUSING", 0.18f, 0.70f, getBuildingIcon("Freddy")));
                pois.add(new MapLocationModel(4, "Curtiss Hall", "JOB", 0.52f, 0.22f, getBuildingIcon("Curtiss")));
                pois.add(new MapLocationModel(5, "Parks Library", "LIBRARY", 0.80f, 0.28f, getBuildingIcon("Parks")));
                pois.add(new MapLocationModel(6, "State Gym", "GYM", 0.78f, 0.75f, getBuildingIcon("State Gym")));
            } else {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    int id = o.optInt("id", i + 1);
                    String name = o.optString("name", "POI " + id);
                    String category = o.optString("category", "GENERIC");

                    double rawX = o.has("xPercent") ? o.optDouble("xPercent")
                            : o.has("xpercent") ? o.optDouble("xpercent")
                            : o.has("x") ? o.optDouble("x")
                            : o.has("x_pct") ? o.optDouble("x_pct")
                            : 0.5;
                    double rawY = o.has("yPercent") ? o.optDouble("yPercent")
                            : o.has("ypercent") ? o.optDouble("ypercent")
                            : o.has("y") ? o.optDouble("y")
                            : o.has("y_pct") ? o.optDouble("y_pct")
                            : 0.5;

                    float xp = normPercent(rawX);
                    float yp = normPercent(rawY);

                    int iconRes = getBuildingIcon(name);
                    pois.add(new MapLocationModel(id, name, category, xp, yp, iconRes));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Location parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        drawPois();
    }

    private void drawPois() {
        overlay.post(() -> {
            overlay.removeAllViews();
            int w = overlay.getWidth();
            int h = overlay.getHeight();

            if (w == 0 || h == 0) {
                overlay.post(this::drawPois);
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(this);
            
            // Scale icon size based on screen width (larger on tablets)
            int baseIconSize = dp(64);
            int screenWidthDp = (int) (w / getResources().getDisplayMetrics().density);
            int iconSize = screenWidthDp >= 600 ? dp(80) : baseIconSize;
            int markerPadding = dp(28);
            int minTouchSize = dp(44); // Minimum touch target

            for (MapLocationModel m : pois) {
                View marker = inflater.inflate(R.layout.view_poi_marker, overlay, false);
                ImageView icon = marker.findViewById(R.id.icon);
                TextView buildingNameView = marker.findViewById(R.id.building_name);
                
                icon.setImageResource(m.getIconResId());
                
                // Set building name
                String buildingName = m.getName();
                if (buildingNameView != null) {
                    buildingNameView.setText(buildingName);
                }
                
                // Update icon size for larger screens
                ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
                iconParams.width = iconSize;
                iconParams.height = iconSize;
                icon.setLayoutParams(iconParams);
                
                // Ensure minimum touch target
                int touchPadding = Math.max(0, (minTouchSize - iconSize) / 2);
                marker.setPadding(touchPadding, touchPadding, touchPadding, touchPadding);
                
                // Enable hardware acceleration for smooth animations
                marker.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                
                // Set content description for accessibility
                marker.setContentDescription(buildingName + ". Tap to enter.");
                icon.setContentDescription(buildingName);

                // Calculate position (accounting for padding and ensuring bounds)
                // Measure marker to get actual size including text
                marker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int markerWidth = marker.getMeasuredWidth();
                int markerHeight = marker.getMeasuredHeight();
                
                float centerX = m.getXPercent() * w;
                float centerY = m.getYPercent() * h;
                
                // Position marker centered on the percentage point
                float x = centerX - markerWidth / 2f;
                float y = centerY - markerHeight / 2f;
                
                // Clamp to ensure marker stays within overlay bounds
                float minX = 0;
                float minY = 0;
                float maxX = w - markerWidth;
                float maxY = h - markerHeight;
                
                x = Math.max(minX, Math.min(maxX, x));
                y = Math.max(minY, Math.min(maxY, y));

                marker.setX(x);
                marker.setY(y);

                // Store reference to marker
                marker.setTag(m);
                marker.setOnClickListener(v -> {
                    if (!isNavigating) {
                        onPoiClicked(m, marker);
                    }
                });

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                overlay.addView(marker, params);
            }
        });
    }

    private void onPoiClicked(MapLocationModel m, View marker) {
        // Prevent double navigation
        if (isNavigating) return;
        
        // Single click - navigate immediately
        performHaptic(HAPTIC_CONFIRM);
        navigateToBuilding(m);
    }
    
    private void performHaptic(int strength) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(strength, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(strength);
            }
        } catch (Exception e) {
            // Ignore vibration errors
        }
    }
    
    // Removed showHintChip, showGlow, and clearGlowAndHint - no longer needed for single-click
    
    /**
     * Centralized building navigation helper with defensive checks.
     */
    private void navigateToBuilding(MapLocationModel m) {
        // Prevent double navigation
        if (isNavigating) return;
        
        // Defensive check for required extras
        if (userId <= 0 || email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isNavigating = true;
        
        String cat = (m.getCategory() == null) ? "" : m.getCategory().trim();
        String name = m.getName().toLowerCase();

        try {
            // Parks Library → QuizActivity
            if ("LIBRARY".equalsIgnoreCase(cat) || name.contains("parks") || name.contains("library")) {
                openBuilding(QuizActivity.class, m.getName());
                return;
            }

            // Curtiss Hall → CurtissArcadeActivity
            if ("JOB".equalsIgnoreCase(cat) || name.contains("curtiss")) {
                openBuilding(CurtissArcadeActivity.class, m.getName());
                return;
            }

            // State Gym → WellnessActivity
            if ("GYM".equalsIgnoreCase(cat) || name.contains("gym") || name.contains("state")) {
                openBuilding(WellnessActivity.class, m.getName());
                return;
            }

            // Freddy Court → FreddyRoomActivity
            if ("HOUSING".equalsIgnoreCase(cat) || name.contains("freddy")) {
                openBuilding(FreddyRoomActivity.class, m.getName());
                return;
            }

            // Beardshear Hall → FinanceHubActivity
            if ("FINANCE".equalsIgnoreCase(cat) || name.contains("beardshear")) {
                openBuilding(FinanceHubActivity.class, m.getName());
                return;
            }

            // Memorial Union → MemorialUnionStoreActivity
            if ("STORE".equalsIgnoreCase(cat) || name.contains("memorial") || name.contains("union")) {
                openBuilding(MemorialUnionStoreActivity.class, m.getName());
                return;
            }
        } catch (Exception e) {
            // If navigation fails, reset flag
            isNavigating = false;
            Toast.makeText(this, "Unable to open " + m.getName() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If no match, reset navigation flag
        isNavigating = false;
    }
    
    /**
     * Helper method to safely open building activities with required extras.
     */
    private void openBuilding(Class<?> target, String buildingName) {
        safeStart(target, buildingName);
    }
    
    /**
     * Safe navigation helper with defensive checks and error handling.
     */
    private void safeStart(Class<?> cls, String buildingName) {
        try {
            int uid = getIntent().getIntExtra("USER_ID", -1);
            if (uid <= 0) uid = cycredit.io.util.UserPrefs.userId(this);
            if (uid <= 0) uid = userId;
            
            String em = getIntent().getStringExtra("EMAIL");
            if (em == null || em.trim().isEmpty()) em = email;
            
            String un = getIntent().getStringExtra("USERNAME");
            if (un == null || un.trim().isEmpty()) un = username;
            if (un == null || un.trim().isEmpty()) un = Session.getUsername(this);
            
            if (uid <= 0) {
                Toast.makeText(this, "Missing user data.", Toast.LENGTH_SHORT).show();
                isNavigating = false;
                return;
            }
            
            Intent i = new Intent(this, cls);
            i.putExtra("USER_ID", uid);
            i.putExtra("EMAIL", em);
            if (un != null && !un.trim().isEmpty()) {
                i.putExtra("USERNAME", un);
            }
            i.putExtra("BUILDING_NAME", buildingName);
            startActivity(i);
            isNavigating = false;
        } catch (Exception e) {
            isNavigating = false;
            Toast.makeText(this, "Screen unavailable: " + buildingName, Toast.LENGTH_LONG).show();
            android.util.Log.e("MapActivity", "Open failed: " + cls.getName(), e);
        }
    }
    
    private int getBuildingIcon(String name) {
        if (name == null) return R.drawable.ic_pokestop;
        String lower = name.toLowerCase();
        if (lower.contains("beardshear")) return R.drawable.ic_beardshear_hall;
        if (lower.contains("curtiss")) return R.drawable.ic_curtiss_hall;
        if (lower.contains("freddy")) return R.drawable.ic_freddy_court;
        if (lower.contains("memorial") || lower.contains("union")) return R.drawable.ic_memorial_union;
        if (lower.contains("parks") || lower.contains("library")) return R.drawable.ic_parks_library;
        if (lower.contains("gym") || lower.contains("state")) return R.drawable.ic_state_gym;
        return R.drawable.ic_pokestop;
    }


    private void completeWelcomeQuest() {
        if (userId <= 0) return;   // safety

        // Only once per user
        String prefName = "quest_welcome_flags";
        String flagKey  = "welcome_q1_user_" + userId;
        if (getSharedPreferences(prefName, MODE_PRIVATE).getBoolean(flagKey, false)) return;

        String url = ApiClient.BASE_URL + "/api/quests/q1/complete?userId=" + userId;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    getSharedPreferences(prefName, MODE_PRIVATE).edit().putBoolean(flagKey, true).apply();
                    Toast.makeText(MapActivity.this, "Welcome Aboard quest completed!", Toast.LENGTH_SHORT).show();
                    awardLeaderboardPointsForWelcome(100);
                },
                error -> { /* optional log */ }
        );
        ApiClient.getRequestQueue(this).add(req);
    }

    private void awardLeaderboardPointsForWelcome(int delta) {
        if (userId <= 0 || delta <= 0) return;

        String url = ApiClient.BASE_URL + "/api/leaderboard/add";
        try {
            JSONObject body = new JSONObject();
            body.put("userId", String.valueOf(userId));

            String displayName = Session.getUsername(this);
            if (displayName == null || displayName.isEmpty()) {
                if (email != null && !email.isEmpty()) {
                    displayName = email;
                } else {
                    displayName = "User " + userId;
                }
            }
            body.put("displayName", displayName);
            body.put("delta", delta);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    body,
                    response -> { /* live WS updates will reflect on leaderboard */ },
                    error -> { /* optional log */ }
            );
            ApiClient.getRequestQueue(this).add(req);
        } catch (Exception ignore) { }
    }

    private void showSettingsDialog() {
        String[] options = {
            "View Quests",
            "View Achievements",
            "Change Password",
            "About"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // View Quests
                        Intent questsIntent = new Intent(this, QuestsActivity.class);
                        questsIntent.putExtra("USER_ID", userId);
                        questsIntent.putExtra("EMAIL", email);
                        startActivity(questsIntent);
                        break;
                    case 1: // View Achievements
                        Intent achievementsIntent = new Intent(this, AchievementsActivity.class);
                        achievementsIntent.putExtra("USER_ID", userId);
                        achievementsIntent.putExtra("EMAIL", email);
                        startActivity(achievementsIntent);
                        break;
                    case 2: // Change Password
                        showChangePasswordDialog();
                        break;
                    case 3: // About
                        showAboutDialog();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showChangePasswordDialog() {
        android.view.View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", (d, w) -> {
                android.widget.EditText oldPass = dialogView.findViewById(R.id.old_pass_edt);
                android.widget.EditText newPass = dialogView.findViewById(R.id.new_pass_edt);
                android.widget.EditText confirmPass = dialogView.findViewById(R.id.confirm_pass_edt);
                
                if (oldPass != null && newPass != null && confirmPass != null) {
                    String oldP = oldPass.getText().toString();
                    String newP = newPass.getText().toString();
                    String confirmP = confirmPass.getText().toString();
                    
                    if (newP.equals(confirmP) && !newP.isEmpty()) {
                        // Validate password strength
                        if (newP.length() < 6) {
                            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Call password change API
                        changePassword(oldP, newP);
                    } else {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
        dialog.show();
    }

    private void changePassword(String oldPassword, String newPassword) {
        // Get current user info
        int currentUserId = userId > 0 ? userId : Session.getUserId(this);
        if (currentUserId <= 0) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Build request body with user object (backend expects user with current password)
        JSONObject userBody = new JSONObject();
        try {
            userBody.put("id", currentUserId);
            userBody.put("password", oldPassword); // Current password for validation
        } catch (org.json.JSONException e) {
            Toast.makeText(this, "Failed to build request", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Build URL with query parameters (backend expects oldPassword and newPassword as params)
        String url = ApiClient.BASE_URL + "/users?oldPassword=" + 
                     java.net.URLEncoder.encode(oldPassword, java.nio.charset.StandardCharsets.UTF_8) +
                     "&newPassword=" + 
                     java.net.URLEncoder.encode(newPassword, java.nio.charset.StandardCharsets.UTF_8);
        
        // Backend returns JSON: {"message":"success"} or {"message":"failure"}
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.PUT,
            url,
            userBody,
            response -> {
                // Backend returns JSON with "message" field
                String message = response.optString("message", "");
                if ("success".equalsIgnoreCase(message)) {
                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Password change failed. Please check your old password.", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                String errorMsg = "Failed to change password. Please check your old password.";
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    try {
                        String body = new String(error.networkResponse.data, java.nio.charset.StandardCharsets.UTF_8);
                        JSONObject json = new JSONObject(body);
                        errorMsg = json.optString("message", errorMsg);
                    } catch (Exception e) {
                        // Use default message
                    }
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        );
        
        // Use ApiClient's request queue
        ApiClient.getRequestQueue(this).add(request);
    }
    
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("About CyCredit.io")
            .setMessage("CyCredit.io - Financial Literacy Game\n\n" +
                       "Version: 1.0\n" +
                       "Build your credit score and financial knowledge through gameplay!")
            .setPositiveButton("OK", null)
            .show();
    }
    
    /**
     * Fetch user's selected avatar and display it in the header
     */
    private void fetchUserAvatar() {
        ImageView avatarView = findViewById(R.id.header_avatar);
        if (avatarView == null) return;
        
        int currentUserId = userId > 0 ? userId : Session.getUserId(this);
        if (currentUserId <= 0) return;
        
        String url = ApiClient.BASE_URL + "/avatar/" + currentUserId;
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    String avatarName = response.optString("avatarName", null);
                    if (avatarName != null && !avatarName.isEmpty()) {
                        int avatarResId = getAvatarDrawable(avatarName);
                        if (avatarResId != 0) {
                            avatarView.setImageResource(avatarResId);
                            avatarView.setBackground(null); // Remove default background
                        }
                    }
                } catch (Exception e) {
                    // Silent fail - keep default avatar
                }
            },
            error -> {
                // Silent fail - keep default avatar
            }
        );
        ApiClient.getRequestQueue(this).add(request);
    }
    
    /**
     * Map avatar name to drawable resource ID
     */
    private int getAvatarDrawable(String avatarName) {
        if (avatarName == null) return 0;
        
        // Map character names to their drawable resources (same as CharacterSelectionActivity)
        switch (avatarName.toLowerCase()) {
            case "mekhi":
                return R.drawable.ic_mekhi;
            case "ash":
                return R.drawable.ic_ash;
            case "carson":
                return R.drawable.ic_carson;
            case "chase":
                return R.drawable.ic_chase;
            case "dr. mitra":
            case "mitra":
                return R.drawable.ic_mitra;
            case "swarna":
                return R.drawable.ic_swarna;
            case "wendy wintersteen":
            case "wendy":
                return R.drawable.ic_wendy;
            case "cy the cardinal":
            case "cy":
                return R.drawable.ic_cy;
            default:
                return 0; // Return 0 if not found (keeps default icon)
        }
    }

    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }
}
