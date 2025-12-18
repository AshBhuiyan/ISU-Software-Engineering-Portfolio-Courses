package cycredit.io;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cycredit.io.adapter.TransactionAdapter;
import cycredit.io.model.Transaction;
import cycredit.io.Session;

public class FinanceHubActivity extends AppCompatActivity {

    private int userId = -1;
    private String email = null;

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;
    
    // SharedPreferences for pending statement (from month-end rollover)
    private static final String PREFS_PENDING_STATEMENT = "pending_statement";
    private static final String KEY_PENDING_TOTAL_DUE = "pending_total_due_";
    private static final String KEY_PENDING_MIN_DUE = "pending_min_due_";
    private static final String KEY_PENDING_MONTH = "pending_month_";

    private TextView balanceText, monthlySpendText, creditLimitText;
    private RecyclerView txRecycler;
    private TransactionAdapter txAdapter;
    private ProgressBar progress;
    private SwipeRefreshLayout refresher;
    private Button goToStoreBtn, backToMapBtn;
    
    // Statement payment UI
    private com.google.android.material.card.MaterialCardView statementCard;
    private TextView statementTotalDue, statementMinDue, statementStatus;
    private com.google.android.material.textfield.TextInputEditText paymentAmountInput;
    private Button payMinimumBtn, payFullBtn, payCustomBtn;
    
    private Long currentStatementId = null;
    private double currentTotalDue = 0.0;
    private double currentMinDue = 0.0;
    private String currentStatementStatus = "";
    private double cachedMonthlySpend = 0.0; // From billing summary, for comparison

    private final List<Transaction> transactions = new ArrayList<>();
    private RequestQueue queue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finance_hub);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Finance Hub (Beardshear)");
        }

        userId = getIntent().getIntExtra("USER_ID", -1);
        email  = getIntent().getStringExtra("EMAIL");
        
        // Defensive check for required extras
        if (userId <= 0 || email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        queue = Volley.newRequestQueue(this);

        // Entering Beardshear Hall => bump "Explorer" quest (q2) by +50%, only once per user
        bumpExplorerQuestOnceFrom("FINANCE");

        balanceText = findViewById(R.id.balanceText);
        monthlySpendText = findViewById(R.id.monthlySpendText);
        creditLimitText = findViewById(R.id.creditLimitText);
        txRecycler = findViewById(R.id.txRecycler);
        progress = findViewById(R.id.progressBar);
        refresher = findViewById(R.id.swipeRefresh);
        goToStoreBtn = findViewById(R.id.goToStoreBtn);
        backToMapBtn = findViewById(R.id.backToMapBtn);
        
        // Statement payment UI
        statementCard = findViewById(R.id.statementCard);
        statementTotalDue = findViewById(R.id.statementTotalDue);
        statementMinDue = findViewById(R.id.statementMinDue);
        statementStatus = findViewById(R.id.statementStatus);
        paymentAmountInput = findViewById(R.id.paymentAmountInput);
        payMinimumBtn = findViewById(R.id.payMinimumBtn);
        payFullBtn = findViewById(R.id.payFullBtn);
        payCustomBtn = findViewById(R.id.payCustomBtn);

        txAdapter = new TransactionAdapter(transactions);
        txRecycler.setLayoutManager(new LinearLayoutManager(this));
        txRecycler.setAdapter(txAdapter);

        refresher.setOnRefreshListener(this::refreshAll);
        
        // Setup payment button listeners
        if (payMinimumBtn != null) {
            payMinimumBtn.setOnClickListener(v -> payStatement(currentMinDue));
        }
        if (payFullBtn != null) {
            payFullBtn.setOnClickListener(v -> payStatement(currentTotalDue));
        }
        if (payCustomBtn != null) {
            payCustomBtn.setOnClickListener(v -> {
                try {
                    String amountStr = paymentAmountInput.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        cycredit.io.util.ToastHelper.showError(this, "Payment", "Please enter an amount");
                        return;
                    }
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        cycredit.io.util.ToastHelper.showError(this, "Payment", "Amount must be positive");
                        return;
                    }
                    payStatement(amount);
                } catch (NumberFormatException e) {
                    cycredit.io.util.ToastHelper.showError(this, "Payment", "Invalid amount format");
                }
            });
        }

        goToStoreBtn.setOnClickListener(v -> {
            Intent i = new Intent(FinanceHubActivity.this, MemorialUnionStoreActivity.class);
            i.putExtra("USER_ID", userId);
            i.putExtra("EMAIL", email);
            startActivity(i);
        });

        if (backToMapBtn != null) {
            backToMapBtn.setOnClickListener(v -> finish());
        }

        // Setup bottom navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.action_bar);
        if (bottomNav != null) {
            setupBottomNavigation(bottomNav);
        }

        refreshAll();
    }

    private void setupBottomNavigation(com.google.android.material.bottomnavigation.BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_map) {
                Intent i = new Intent(this, MapActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            } else if (itemId == R.id.nav_guilds) {
                int uid = (userId > 0) ? userId : Session.getUserId(this);
                String uname = Session.getUsername(this);
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
                // End turn - navigate to map
                Intent i = new Intent(this, MapActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            }
            return false;
        });
        // Finance Hub doesn't have a direct nav item, so no selection
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    // ==================== PENDING STATEMENT HELPERS ====================
    
    /**
     * Get the pending total due from month-end rollover (stored in SharedPreferences).
     * This is used when the backend statement shows $0 but user had spending.
     */
    private double getPendingTotalDue() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PENDING_STATEMENT, MODE_PRIVATE);
        return Double.longBitsToDouble(prefs.getLong(KEY_PENDING_TOTAL_DUE + userId, Double.doubleToLongBits(0.0)));
    }
    
    /**
     * Get the pending minimum due from month-end rollover.
     */
    private double getPendingMinimumDue() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PENDING_STATEMENT, MODE_PRIVATE);
        return Double.longBitsToDouble(prefs.getLong(KEY_PENDING_MIN_DUE + userId, Double.doubleToLongBits(0.0)));
    }
    
    /**
     * Clear the pending statement after it's been paid or a new month starts.
     */
    private void clearPendingStatement() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PENDING_STATEMENT, MODE_PRIVATE);
        prefs.edit()
            .remove(KEY_PENDING_TOTAL_DUE + userId)
            .remove(KEY_PENDING_MIN_DUE + userId)
            .remove(KEY_PENDING_MONTH + userId)
            .apply();
        Log.d("FinanceHub", "Cleared pending statement for user " + userId);
    }
    
    /**
     * Save pending statement from month-end rollover.
     * Called from MapActivity when month ends.
     */
    public static void savePendingStatement(android.content.Context context, int userId, 
                                            double totalDue, double minimumDue, int monthNumber) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PENDING_STATEMENT, MODE_PRIVATE);
        prefs.edit()
            .putLong(KEY_PENDING_TOTAL_DUE + userId, Double.doubleToLongBits(totalDue))
            .putLong(KEY_PENDING_MIN_DUE + userId, Double.doubleToLongBits(minimumDue))
            .putInt(KEY_PENDING_MONTH + userId, monthNumber)
            .apply();
        Log.d("FinanceHub", "Saved pending statement: totalDue=$" + totalDue + 
            ", minDue=$" + minimumDue + ", month=" + monthNumber);
    }
    
    // ==================== PAYMENT CONTROLS ====================
    
    /**
     * Centralized method to enable/disable payment controls.
     * Call this after every statement fetch, payment, and in onResume.
     * @param enabled Whether controls should be enabled
     * @param reason Reason for logging (e.g., "totalDue > 0", "statement paid", "no statement")
     */
    private void setPaymentControlsEnabled(boolean enabled, String reason) {
        Log.d("FinanceHub", "setPaymentControlsEnabled(" + enabled + ") - reason: " + reason);
        
        // Hide progress bar first to ensure no overlay blocks touches
        progress.setVisibility(View.GONE);
        
        if (payMinimumBtn != null) {
            payMinimumBtn.setEnabled(enabled && currentMinDue > 0);
            payMinimumBtn.setAlpha(enabled && currentMinDue > 0 ? 1.0f : 0.5f);
        }
        if (payFullBtn != null) {
            payFullBtn.setEnabled(enabled);
            payFullBtn.setAlpha(enabled ? 1.0f : 0.5f);
        }
        if (payCustomBtn != null) {
            payCustomBtn.setEnabled(enabled);
            payCustomBtn.setAlpha(enabled ? 1.0f : 0.5f);
        }
        if (paymentAmountInput != null) {
            paymentAmountInput.setEnabled(enabled);
            paymentAmountInput.setFocusable(enabled);
            paymentAmountInput.setFocusableInTouchMode(enabled);
            paymentAmountInput.setClickable(enabled);
            paymentAmountInput.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }
    
    /**
     * Update UI based on current statement state.
     * Centralizes all the UI update logic.
     */
    private void updateStatementUI() {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        
        statementTotalDue.setText(currency.format(currentTotalDue));
        statementMinDue.setText(currency.format(currentMinDue));
        
        // Determine if payment is possible
        boolean canPay = currentStatementId != null && 
                         currentStatementId > 0 && 
                         !"PAID".equals(currentStatementStatus) && 
                         currentTotalDue > 0;
        
        // Update status text
        if (currentTotalDue <= 0) {
            if (cachedMonthlySpend > 0) {
                statementStatus.setText("Status: " + currentStatementStatus + 
                    "\n(Monthly spend: $" + String.format("%.2f", cachedMonthlySpend) + 
                    " - bill at month end)");
            } else {
                statementStatus.setText("Status: " + currentStatementStatus + " (No payment due)");
            }
        } else {
            statementStatus.setText("Status: " + currentStatementStatus);
        }
        
        // Enable/disable controls
        setPaymentControlsEnabled(canPay, 
            "canPay=" + canPay + ", totalDue=" + currentTotalDue + ", status=" + currentStatementStatus);
        
        statementCard.setVisibility(View.VISIBLE);
    }

    private void refreshAll() {
        progress.setVisibility(View.VISIBLE);
        loadSummary();
        loadTransactions();
        loadCurrentStatement();
    }
    
    private void loadCurrentStatement() {
        String url = BASE_URL + "/statements/current?userId=" + userId;
        Log.d("FinanceHub", "=== LOADING CURRENT STATEMENT ===");
        Log.d("FinanceHub", "URL: " + url);
        
        // Check for pending statement from month-end rollover
        double pendingTotalDue = getPendingTotalDue();
        double pendingMinDue = getPendingMinimumDue();
        Log.d("FinanceHub", "Pending statement: totalDue=$" + pendingTotalDue + ", minDue=$" + pendingMinDue);
        
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        // DEBUG: Log the COMPLETE raw JSON response
                        Log.d("FinanceHub", "=== STATEMENT API RAW RESPONSE ===");
                        Log.d("FinanceHub", response.toString(2)); // Pretty print
                        
                        // DEBUG: Log all keys and their values
                        java.util.Iterator<String> keys = response.keys();
                        Log.d("FinanceHub", "=== ALL FIELDS IN RESPONSE ===");
                        while (keys.hasNext()) {
                            String key = keys.next();
                            Object value = response.opt(key);
                            Log.d("FinanceHub", "  Field '" + key + "' = " + value + 
                                " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
                        }
                        
                        // Check if there's a current statement
                        if (response.has("message") && "No current statement".equals(response.optString("message"))) {
                            Log.d("FinanceHub", "No current statement from backend");
                            handleNoStatement(pendingTotalDue, pendingMinDue);
                            return;
                        }
                        
                        currentStatementId = response.optLong("id", 0);
                        
                        // Parse total due - check multiple possible field names
                        currentTotalDue = parseAmountFromResponse(response, 
                            "remainingBalance", "remaining_balance",
                            "balanceDue", "balance_due", 
                            "amountDue", "amount_due",
                            "totalDue", "total_due",
                            "currentDue", "current_due",
                            "balance", "total", "due");
                        
                        // Parse minimum due
                        currentMinDue = parseAmountFromResponse(response,
                            "minimumDue", "minimum_due",
                            "minDue", "min_due",
                            "minimumPayment", "minimum_payment", 
                            "minPayment", "min_payment");
                        
                        currentStatementStatus = response.optString("status", "OPEN");
                        
                        Log.d("FinanceHub", "=== PARSED FROM BACKEND ===");
                        Log.d("FinanceHub", "  statementId = " + currentStatementId);
                        Log.d("FinanceHub", "  totalDue = " + currentTotalDue);
                        Log.d("FinanceHub", "  minimumDue = " + currentMinDue);
                        Log.d("FinanceHub", "  status = " + currentStatementStatus);
                        
                        // === FIX: Only use pending values if we do NOT have a real backend statement ===
                        // If we have a valid statement ID from backend, TRUST it (even if totalDue is 0 = paid)
                        // Only fallback to pending values when there's no valid backend statement
                        if (currentStatementId <= 0 && pendingTotalDue > 0) {
                            Log.d("FinanceHub", "No valid backend statement ID, using pending values: $" + pendingTotalDue);
                            currentTotalDue = pendingTotalDue;
                            currentMinDue = pendingMinDue;
                            currentStatementId = -1L; // Mark as pending statement
                        } else if (currentStatementId > 0) {
                            // We have a valid backend statement - trust its values
                            Log.d("FinanceHub", "Valid backend statement ID=" + currentStatementId + 
                                ", trusting backend totalDue=$" + currentTotalDue);
                            // If backend shows paid (totalDue=0), clear any stale pending values
                            if (currentTotalDue <= 0.01 && pendingTotalDue > 0) {
                                Log.d("FinanceHub", "Statement is paid, clearing stale pending values");
                                clearPendingStatement();
                            }
                        }
                        
                        // === APPLY INVARIANTS ===
                        if (currentTotalDue <= 0 && currentMinDue > 0) {
                            Log.w("FinanceHub", "INVARIANT FIX: totalDue=0 but minimumDue=" + currentMinDue);
                            currentMinDue = 0.0;
                        }
                        if (currentMinDue > currentTotalDue) {
                            Log.w("FinanceHub", "INVARIANT FIX: minDue > totalDue, clamping");
                            currentMinDue = currentTotalDue;
                        }
                        
                        Log.d("FinanceHub", "=== FINAL VALUES ===");
                        Log.d("FinanceHub", "  totalDue = " + currentTotalDue);
                        Log.d("FinanceHub", "  minimumDue = " + currentMinDue);
                        
                        // Update UI using centralized method
                        updateStatementUI();
                        cycredit.io.util.ToastHelper.showRead(this, "current statement", 1);
                        
                    } catch (Exception e) {
                        Log.e("FinanceHub", "Error parsing statement", e);
                        cycredit.io.util.ToastHelper.showError(this, "Parse statement", e.getMessage());
                        handleNoStatement(pendingTotalDue, pendingMinDue);
                    }
                },
                error -> {
                    Log.d("FinanceHub", "Statement fetch error: " + 
                        cycredit.io.util.ErrorHandler.getErrorMessage(error));
                    handleNoStatement(pendingTotalDue, pendingMinDue);
                }
        );
        queue.add(req);
    }
    
    /**
     * Handle the case where there's no statement from backend.
     * Uses pending statement values if available.
     */
    private void handleNoStatement(double pendingTotalDue, double pendingMinDue) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        
        // Use pending statement if available
        if (pendingTotalDue > 0) {
            Log.d("FinanceHub", "Using pending statement: $" + pendingTotalDue);
            currentStatementId = -1L; // Marker for pending statement
            currentTotalDue = pendingTotalDue;
            currentMinDue = pendingMinDue;
            currentStatementStatus = "PENDING";
            
            statementTotalDue.setText(currency.format(currentTotalDue));
            statementMinDue.setText(currency.format(currentMinDue));
            statementStatus.setText("Status: Statement pending\n(Waiting for backend sync - try refreshing)");
            
            // Can't pay pending statement without real ID
            setPaymentControlsEnabled(false, "pending statement - no backend ID");
            statementCard.setVisibility(View.VISIBLE);
            
        } else if (cachedMonthlySpend > 0) {
            // No pending statement, but there's monthly spend
            Log.d("FinanceHub", "No statement but monthlySpend=$" + cachedMonthlySpend);
            currentStatementId = null;
            currentTotalDue = 0.0;
            currentMinDue = 0.0;
            currentStatementStatus = "NONE";
            
            statementTotalDue.setText(currency.format(0));
            statementMinDue.setText(currency.format(0));
            statementStatus.setText("Status: No statement yet\n(Monthly spend: $" + 
                String.format("%.2f", cachedMonthlySpend) + " - bill at month end)");
            
            setPaymentControlsEnabled(false, "no statement yet");
            statementCard.setVisibility(View.VISIBLE);
            
        } else {
            // No statement and no spending
            Log.d("FinanceHub", "No statement and no spending");
            setPaymentControlsEnabled(false, "no statement");
            statementCard.setVisibility(View.GONE);
        }
    }
    
    /**
     * Parse an amount from JSON response, checking multiple possible field names.
     * Returns the first value found (including explicit zeros), or 0.0 if no field exists.
     */
    private double parseAmountFromResponse(JSONObject response, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (response.has(fieldName)) {
                double value = response.optDouble(fieldName, Double.NaN);
                if (!Double.isNaN(value)) {
                    Log.d("FinanceHub", "parseAmount: Found '" + fieldName + "' = " + value);
                    return value;
                }
                // Try parsing as string in case it's a string number
                String strValue = response.optString(fieldName, "");
                if (!strValue.isEmpty()) {
                    try {
                        value = Double.parseDouble(strValue);
                        Log.d("FinanceHub", "parseAmount: Found '" + fieldName + "' (as string) = " + value);
                        return value;
                    } catch (NumberFormatException ignored) {
                        Log.w("FinanceHub", "parseAmount: Field '" + fieldName + "' exists but not a number: " + strValue);
                    }
                }
            }
        }
        Log.d("FinanceHub", "parseAmount: No matching field found for: " + String.join(", ", fieldNames));
        return 0.0;
    }
    
    private void payStatement(double amount) {
        Log.d("FinanceHub", "=== PAYMENT ATTEMPT ===");
        Log.d("FinanceHub", "  requested amount: " + amount);
        Log.d("FinanceHub", "  currentStatementId: " + currentStatementId);
        Log.d("FinanceHub", "  currentTotalDue: " + currentTotalDue);
        Log.d("FinanceHub", "  currentMinDue: " + currentMinDue);
        Log.d("FinanceHub", "  currentStatementStatus: " + currentStatementStatus);
        
        // Validation 1: Must have a statement
        if (currentStatementId == null || currentStatementId == 0) {
            Log.w("FinanceHub", "Payment blocked: No statement ID");
            cycredit.io.util.ToastHelper.showError(this, "Payment", "No statement available");
            return;
        }
        
        // Validation 2: Statement must not be already paid
        if ("PAID".equals(currentStatementStatus)) {
            Log.w("FinanceHub", "Payment blocked: Statement already paid");
            cycredit.io.util.ToastHelper.showError(this, "Payment", "Statement is already paid");
            return;
        }
        
        // Validation 3: Must have something to pay (totalDue > 0)
        if (currentTotalDue <= 0) {
            Log.w("FinanceHub", "Payment blocked: No balance due (totalDue=" + currentTotalDue + ")");
            Toast.makeText(this, "No payment due right now", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validation 4: Amount must be positive
        if (amount <= 0) {
            Log.w("FinanceHub", "Payment blocked: Amount not positive (" + amount + ")");
            cycredit.io.util.ToastHelper.showError(this, "Payment", "Amount must be positive");
            return;
        }
        
        // Validation 5: Amount cannot exceed total due
        if (amount > currentTotalDue + 0.01) { // small epsilon for floating point
            Log.w("FinanceHub", "Payment blocked: Amount " + amount + " exceeds totalDue " + currentTotalDue);
                cycredit.io.util.ToastHelper.showError(this, "Payment", 
                    String.format(Locale.US, "Payment cannot exceed $%.2f", currentTotalDue));
                return;
            }
        
        // Validation 6: REMOVED minimum due requirement for demo flexibility
        // Users can pay any amount from $0.01 up to totalDue
        // In a real app, you'd enforce: amount >= currentMinDue
        Log.d("FinanceHub", "Custom amount allowed: any amount up to totalDue (demo mode)");
        
        Log.d("FinanceHub", "All validations passed, proceeding with payment");
        String url = BASE_URL + "/statements/" + currentStatementId + "/pay";
        
        try {
            JSONObject body = new JSONObject();
            body.put("amount", amount);
            
            Log.d("FinanceHub", "Sending payment request to: " + url + " with body: " + body.toString());
            
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    body,
                    response -> {
                        Log.d("FinanceHub", "Payment response: " + response.toString());
                        
                        // Check for success - handle both explicit success field and absence of error
                        boolean success = response.optBoolean("success", !response.has("error"));
                        if (success) {
                            double paidAmount = response.optDouble("amount", amount);
                            // Parse remaining balance from multiple possible field names
                            double remaining = parseAmountFromResponse(response,
                                "remainingBalance", "remaining_balance", "remaining",
                                "balanceDue", "balance_due", "totalDue", "total_due");
                            String status = response.optString("status", currentStatementStatus);
                            
                            NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
                            String message = String.format(Locale.US, 
                                "Payment successful! Paid %s. Remaining: %s", 
                                currency.format(paidAmount), currency.format(remaining));
                            
                            cycredit.io.util.ToastHelper.showUpdate(this, "statement payment");
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            
                            // Clear the custom amount input
                            if (paymentAmountInput != null) {
                                paymentAmountInput.setText("");
                            }
                            
                            // Clear pending statement if fully paid
                            if (remaining <= 0.01 || "PAID".equals(status)) {
                                Log.d("FinanceHub", "Statement fully paid, clearing pending");
                                clearPendingStatement();
                            }
                            
                            // Refresh data
                            refreshAll();
                            HudSyncHelper.refreshHud(this, userId);
                        } else {
                            String errorMsg = response.optString("error", 
                                response.optString("message", "Payment failed"));
                            cycredit.io.util.ToastHelper.showError(this, "Payment", errorMsg);
                        }
                    },
                    error -> {
                        Log.e("FinanceHub", "Payment error", error);
                        cycredit.io.util.ToastHelper.showError(this, "Payment", 
                            cycredit.io.util.ErrorHandler.getErrorMessage(error));
                    }
            );
            
            queue.add(req);
        } catch (JSONException e) {
            Log.e("FinanceHub", "Error creating payment request", e);
            cycredit.io.util.ToastHelper.showError(this, "Payment", "Error creating payment request");
        }
    }

    private void loadSummary() {
        String url = BASE_URL + "/billing/summary/" + userId;
        Log.d("FinanceHub", "=== LOADING BILLING SUMMARY ===");
        Log.d("FinanceHub", "URL: " + url);
        
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        Log.d("FinanceHub", "Billing summary response: " + response.toString());
                        
                        double balance = response.optDouble("balance", 0.0);
                        double monthly = response.optDouble("monthlySpend", 0.0);
                        double limit = response.optDouble("creditLimit", 1000.0);
                        
                        // Cache monthly spend for comparison with statement
                        cachedMonthlySpend = monthly;
                        Log.d("FinanceHub", "Cached monthlySpend: $" + cachedMonthlySpend);
                        
                        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

                        balanceText.setText(currency.format(balance));
                        monthlySpendText.setText(currency.format(monthly));
                        creditLimitText.setText(currency.format(limit));
                        cycredit.io.util.ToastHelper.showRead(this, "billing summary", 1);
                    } catch (Exception e) {
                        Log.e("FinanceHub", "Error parsing billing summary", e);
                        cycredit.io.util.ToastHelper.showError(this, "Parse summary", e.getMessage());
                    } finally {
                        progress.setVisibility(View.GONE);
                        refresher.setRefreshing(false);
                    }
                },
                error -> {
                    Log.w("FinanceHub", "Billing summary failed: " + cycredit.io.util.ErrorHandler.getErrorMessage(error));
                    cycredit.io.util.ToastHelper.showError(this, "Fetch summary", cycredit.io.util.ErrorHandler.getErrorMessage(error));

                    NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
                    balanceText.setText(currency.format(125.73));
                    monthlySpendText.setText(currency.format(342.18));
                    creditLimitText.setText(currency.format(1500.00));
                    cachedMonthlySpend = 342.18; // Mock value
                    Toast.makeText(this, "Using mock summary (enable /billing/summary/{id})", Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                    refresher.setRefreshing(false);
                }
        );
        queue.add(req);
    }

    private void loadTransactions() {
        String url = BASE_URL + "/billing/transactions?userId=" + userId;
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        transactions.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject o = response.getJSONObject(i);
                            transactions.add(Transaction.fromJson(o));
                        }
                        txAdapter.notifyDataSetChanged();
                        cycredit.io.util.ToastHelper.showList(this, "transactions", transactions.size());
                    } catch (JSONException e) {
                        cycredit.io.util.ToastHelper.showError(this, "Parse transactions", e.getMessage());
                    } finally {
                        progress.setVisibility(View.GONE);
                        refresher.setRefreshing(false);
                    }
                },
                error -> {
                    cycredit.io.util.ToastHelper.showError(this, "Fetch transactions", cycredit.io.util.ErrorHandler.getErrorMessage(error));

                    transactions.clear();
                    // Mock data using normalized model (all positive amounts, type determines sign)
                    transactions.add(new Transaction("Memorial Union Market", 8.99, "2025-10-27T14:02:00Z", "Snack", "PURCHASE"));
                    transactions.add(new Transaction("MU Bookstore", 21.50, "2025-10-27T13:41:00Z", "Supplies", "PURCHASE"));
                    transactions.add(new Transaction("Statement Payment", 50.00, "2025-10-26T19:08:00Z", "Payment", "PAYMENT"));
                    txAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Using mock transactions (enable /billing/transactions)", Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                    refresher.setRefreshing(false);
                }
        );
        queue.add(req);
    }

    private void bumpExplorerQuestOnceFrom(String sourceKey) {
        // No user, no update
        if (userId <= 0) return;

        // Per–user, per–source key
        String prefName = "quest_explorer_flags";
        String flagKey  = "explorer_" + sourceKey + "_user_" + userId;

        android.content.SharedPreferences prefs =
                getSharedPreferences(prefName, MODE_PRIVATE);

        // If we've already counted this location for this user, do nothing
        if (prefs.getBoolean(flagKey, false)) {
            return;
        }

        String url = BASE_URL + "/api/quests/q2/tick?userId=" + userId + "&delta=50";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    // Mark this location as used for this user
                    prefs.edit().putBoolean(flagKey, true).apply();
                    cycredit.io.util.ToastHelper.showUpdate(this, "explorer quest progress");
                    // Also award leaderboard points for visiting Finance Hub
                    awardLeaderboardPoints(125); // for now I am keeping the score to 125; in future we are gonna change it
                },
                error -> {
                    cycredit.io.util.ToastHelper.showError(this, "Update quest", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                }
        );

        queue.add(req);
    }

    private void awardLeaderboardPoints(int delta) {
        if (userId <= 0 || delta <= 0) return;

        String url = BASE_URL + "/api/leaderboard/add";

        try {
            JSONObject body = new JSONObject();
            body.put("userId", String.valueOf(userId));

            // Try to use username; fall back to email or generic label
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
                    response -> {
                        cycredit.io.util.ToastHelper.showUpdate(this, "leaderboard points");
                    },
                    error -> {
                        cycredit.io.util.ToastHelper.showError(this, "Update leaderboard", cycredit.io.util.ErrorHandler.getErrorMessage(error));
                    }
            );

            queue.add(req);
        } catch (Exception e) {
            // Don’t break FinanceHub if leaderboard fails
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }
}
