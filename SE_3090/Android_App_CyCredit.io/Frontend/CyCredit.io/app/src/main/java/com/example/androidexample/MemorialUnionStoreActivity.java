package cycredit.io;

import android.content.Intent;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import cycredit.io.adapter.SectionedStoreAdapter;
import cycredit.io.model.StoreItem;
import android.util.Log;

import cycredit.io.util.ErrorHandler;
import cycredit.io.Session;

public class MemorialUnionStoreActivity extends AppCompatActivity implements SectionedStoreAdapter.OnPurchaseClick {

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;

    private int userId;
    private String email;

    private RecyclerView recycler;
    private ProgressBar progress;
    private SwipeRefreshLayout refresher;
    private TextView emptyText;
    private Button backToMapBtn;

    private TextView availableBalance;
    private double currentMoney = 0.0;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

    private SectionedStoreAdapter adapter;
    private final List<StoreItem> items = new ArrayList<>();
    private RequestQueue queue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memorial_union_store);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Memorial Union Store");
        }

        userId = getIntent().getIntExtra("USER_ID", 1);
        email  = getIntent().getStringExtra("EMAIL");

        queue  = Volley.newRequestQueue(this);

        // First time user visits store → +50% Explorer
        bumpExplorerQuestOnceFrom("STORE");

        recycler   = findViewById(R.id.storeRecycler);
        progress   = findViewById(R.id.storeProgress);
        refresher  = findViewById(R.id.storeSwipe);
        emptyText  = findViewById(R.id.emptyText);
        backToMapBtn = findViewById(R.id.backToMapBtn);
        availableBalance = findViewById(R.id.availableBalance);

        adapter = new SectionedStoreAdapter(items, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        refresher.setOnRefreshListener(() -> {
            loadItems();
            fetchResourceBalance();
        });

        if (backToMapBtn != null) {
            backToMapBtn.setOnClickListener(v -> finish());
        }

        // Setup bottom navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.action_bar);
        if (bottomNav != null) {
            setupBottomNavigation(bottomNav);
        }

        loadItems();
        fetchResourceBalance();
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
                Intent i = new Intent(this, LeaderboardActivity.class);
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
        // Store doesn't have a direct nav item, so no selection
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchResourceBalance();
    }

    private void setEmptyStateIfNeeded() {
        if (emptyText != null) {
            emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void loadItems() {
        progress.setVisibility(View.VISIBLE);
        String url = BASE_URL + "/store/memorial-union/items";

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        items.clear();
                        for (int i = 0; i < response.length(); i++) {
                            items.add(StoreItem.fromJson(response.getJSONObject(i)));
                        }
                        adapter.updateData(items);
                        cycredit.io.util.ToastHelper.showList(this, "store items", items.size());
                    } catch (Exception e) {
                        cycredit.io.util.ToastHelper.showError(this, "Parse store items", e.getMessage());
                    } finally {
                        progress.setVisibility(View.GONE);
                        refresher.setRefreshing(false);
                        setEmptyStateIfNeeded();
                    }
                },
                error -> {

                    items.clear();
                    items.add(new StoreItem(1, "MU Coffee", "Hot coffee from MU Market", 2.50));
                    items.add(new StoreItem(2, "MU Sandwich", "Turkey & cheese", 6.99));
                    items.add(new StoreItem(3, "Blue Book", "Exam booklet", 0.99));
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Using mock MU items (enable /store/memorial-union/items)", Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                    refresher.setRefreshing(false);
                    setEmptyStateIfNeeded();
                }
        );
        queue.add(req);
    }

    private void fetchResourceBalance() {
        String url = BASE_URL + "/resource/" + userId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                resp -> {
                    try {
                        JSONObject json = new JSONObject(resp);
                        currentMoney = json.optDouble("money", currentMoney);
                        if (availableBalance != null) {
                            availableBalance.setText("Available: " + currency.format(currentMoney));
                        }
                        cycredit.io.util.ToastHelper.showRead(this, "balance", 1);
                    } catch (Exception e) {
                        cycredit.io.util.ToastHelper.showError(this, "Fetch balance", e.getMessage());
                    }
                },
                err -> {
                    cycredit.io.util.ToastHelper.showError(this, "Fetch balance", ErrorHandler.getErrorMessage(err));
                });
        queue.add(req);
    }

    @Override
    public void onPurchase(StoreItem item) {
        Log.d("MUStore", "=== PURCHASE ATTEMPT ===");
        Log.d("MUStore", "Item: " + item.getName() + ", Price: $" + item.getPrice());
        Log.d("MUStore", "Current balance: $" + currentMoney);

        if (currentMoney < item.getPrice()) {
            Log.w("MUStore", "Insufficient funds: " + currentMoney + " < " + item.getPrice());
            Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        String url = BASE_URL + "/store/memorial-union/purchase";

        try {
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("itemId", item.getId());
            body.put("qty", 1);
            body.put("purchaseNonce", java.util.UUID.randomUUID().toString());
            
            Log.d("MUStore", "Purchase request URL: " + url);
            Log.d("MUStore", "Purchase request body: " + body.toString());

            JsonObjectRequest post = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    body,
                    resp -> {
                        Log.d("MUStore", "=== PURCHASE SUCCESS ===");
                        Log.d("MUStore", "Response: " + resp.toString());
                        
                        // Log transaction details if present
                        if (resp.has("transaction")) {
                            Log.d("MUStore", "Transaction created: " + resp.optJSONObject("transaction"));
                        }
                        if (resp.has("newBalance")) {
                            Log.d("MUStore", "New balance: " + resp.optDouble("newBalance"));
                        }
                        
                        cycredit.io.util.ToastHelper.showCreate(this, item.getName());
                        progress.setVisibility(View.GONE);
                        
                        // Refresh balance locally
                        fetchResourceBalance();
                        
                        // CRITICAL: Refresh the global HUD to sync turns/cash/score
                        HudSyncHelper.refreshHud(this, userId);
                        
                        Log.d("MUStore", "HUD refresh triggered after purchase");
                    },
                    err -> {
                        Log.e("MUStore", "=== PURCHASE FAILED ===");
                        Log.e("MUStore", "Error: " + ErrorHandler.getErrorMessage(err));
                        
                        progress.setVisibility(View.GONE);
                        ErrorHandler.handleError(this, err, "Purchase failed");
                        cycredit.io.util.ToastHelper.showError(this, "Purchase", ErrorHandler.getErrorMessage(err));
                        fetchResourceBalance(); // Refresh balance
                    }
            );
            queue.add(post);

        } catch (JSONException e) {
            Log.e("MUStore", "Build request error", e);
            Toast.makeText(this, "Build request error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progress.setVisibility(View.GONE);
        }
    }

    private void bumpExplorerQuestOnceFrom(String sourceKey) {
        if (userId <= 0) return;

        String prefName = "quest_explorer_flags";
        String flagKey  = "explorer_" + sourceKey + "_user_" + userId;

        android.content.SharedPreferences prefs =
                getSharedPreferences(prefName, MODE_PRIVATE);

        // Already counted this location for this user
        if (prefs.getBoolean(flagKey, false)) {
            return;
        }

        String url = BASE_URL + "/api/quests/q2/tick?userId=" + userId + "&delta=50";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    // Mark this source as used so it only fires once
                    prefs.edit().putBoolean(flagKey, true).apply();

                    // Also award leaderboard points for visiting this location
                    awardLeaderboardPoints(125); // for now I am keeping the score to 125; in future we are gonna change it
                },
                error -> {
                    // Optional: log or toast for debugging
                    // Toast.makeText(this, "Explorer quest update failed", Toast.LENGTH_SHORT).show();
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

            // Try to use the in-app username; fall back to email or generic name
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
                        // No UI needed; the WebSocket broadcast will refresh the leaderboard
                    },
                    error -> {
                        // Optional: log error for debugging
                        // Log.e("Leaderboard", "Failed to award points", error);
                    }
            );

            queue.add(req);
        } catch (Exception e) {
            // Swallow – leaderboard is a bonus feature, don't break the flow
        }
    }
}
