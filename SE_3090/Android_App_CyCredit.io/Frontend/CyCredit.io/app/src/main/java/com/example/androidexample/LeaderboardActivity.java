package cycredit.io;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cycredit.io.R;
import cycredit.io.Session;
import cycredit.io.guilds.ApiClient;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private LeaderboardAdapter adapter;
    private TextView statusText;
    private ProgressBar progressBar;

    private LeaderboardWs ws;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Live Leaderboard");
        }

        recycler    = findViewById(R.id.rv_leaderboard);
        statusText  = findViewById(R.id.txt_status);
        progressBar = findViewById(R.id.progress_bar);

        adapter = new LeaderboardAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // Build WS URL from ApiClient.BASE_URL ("http://host:8080") -> "ws://host:8080/ws/leaderboard"
        String base = ApiClient.BASE_URL; // you already use this elsewhere
        String wsUrl = base.replaceFirst("^http", "ws") + "/ws/leaderboard";

        ws = new LeaderboardWs(wsUrl, new LeaderboardWs.Listener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    statusText.setText("Connected");
                });
            }

            @Override
            public void onDisconnected(String reason) {
                runOnUiThread(() -> statusText.setText("Disconnected: " + reason));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    statusText.setText("Error");
                    Toast.makeText(LeaderboardActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLeaderboardUpdate(List<LeaderboardEntry> entries) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    statusText.setText("Live");
                    adapter.setItems(entries);
                });
            }
        });

        progressBar.setVisibility(ProgressBar.VISIBLE);
        statusText.setText("Connecting...");
        ws.connect();

        // Back to Campus Map button
        android.widget.Button backToMapBtn = findViewById(R.id.backToMapBtn);
        if (backToMapBtn != null) {
            backToMapBtn.setOnClickListener(v -> finish());
        }

        // Setup bottom navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.action_bar);
        if (bottomNav != null) {
            setupBottomNavigation(bottomNav);
        }
    }

    private void setupBottomNavigation(com.google.android.material.bottomnavigation.BottomNavigationView bottomNav) {
        int userId = getIntent().getIntExtra("USER_ID", -1);
        String email = getIntent().getStringExtra("EMAIL");
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_map) {
                Intent i = new Intent(this, MapActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            } else if (itemId == R.id.nav_guilds) {
                int uid = (userId > 0) ? userId : Session.getUserId(this);
                String uname = Session.getUsername(this);
                Intent i1 = new Intent(this, cycredit.io.guilds.GuildsActivity.class);
                i1.putExtra("USER_ID", uid);
                i1.putExtra("EMAIL", email);
                i1.putExtra("USERNAME", uname);
                startActivity(i1);
                return true;
            } else if (itemId == R.id.nav_missions) {
                Intent i = new Intent(this, MissionsActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                startActivity(i);
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                // Already on Leaderboard - do nothing
                return true;
            } else if (itemId == R.id.nav_end_turn) {
                // End turn - navigate to map
                Intent i = new Intent(this, MapActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_leaderboard);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ws != null) ws.close();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
