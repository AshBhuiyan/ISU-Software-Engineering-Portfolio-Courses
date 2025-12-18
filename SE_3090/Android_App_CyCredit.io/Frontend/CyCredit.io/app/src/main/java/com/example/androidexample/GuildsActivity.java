package cycredit.io.guilds;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cycredit.io.GuildService;
import cycredit.io.JoinGuildActivity;
import cycredit.io.R;

public class GuildsActivity extends AppCompatActivity {

    // Constants for intent extras
    public static final String EXTRA_GUILD_ID   = "GUILD_ID";
    public static final String EXTRA_GUILD_NAME = "GUILD_NAME";
    public static final String EXTRA_USER_ID    = "USER_ID";
    public static final String EXTRA_EMAIL      = "EMAIL";
    public static final String EXTRA_USERNAME   = "USERNAME";

    private SwipeRefreshLayout swipe;
    private RecyclerView rv;
    private ProgressBar progress;
    private GuildAdapter adapter;

    private int userId;
    private String username;
    private String email;

    private int safeUserId() {
        int uid = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        if (uid <= 0) {
            try { uid = cycredit.io.util.UserPrefs.userId(this); } catch (Exception ignore) {}
        }
        if (uid <= 0) {
            try { uid = cycredit.io.Session.getUserId(this); } catch (Exception ignore) {}
        }
        return uid;
    }

    private String safeEmail() {
        String e = getIntent().getStringExtra(EXTRA_EMAIL);
        return e != null ? e : "";
    }

    private String safeUsername() {
        String u = getIntent().getStringExtra(EXTRA_USERNAME);
        return u != null ? u : "";
    }

    private void openGuild(int guildId, String guildName) {
        try {
            Class<?> target = cycredit.io.guilds.GuildDetailActivity.class;
            android.content.Intent i = new android.content.Intent(this, target);
            i.putExtra(EXTRA_GUILD_ID, guildId);
            i.putExtra(EXTRA_GUILD_NAME, guildName != null ? guildName : "");
            i.putExtra(EXTRA_USER_ID, safeUserId());
            i.putExtra(EXTRA_EMAIL, safeEmail());
            i.putExtra(EXTRA_USERNAME, safeUsername());
            startActivity(i);
        } catch (Throwable t) {
            android.util.Log.e("GuildsActivity", "Failed to open guild", t);
            android.widget.Toast.makeText(this, "Unable to open guild", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guilds);

        userId = safeUserId();
        username = safeUsername();
        email = safeEmail();

        ImageButton back = findViewById(R.id.btnBack);
        ImageButton add = findViewById(R.id.btnAdd);
        ImageButton join = findViewById(R.id.btnJoin);

        if (back != null) {
            back.setOnClickListener(v -> {
                if (isTaskRoot()) {
                    Intent i = new Intent(this, cycredit.io.LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                } else {
                    finish();
                }
            });
        }

        if (add != null) {
            add.setOnClickListener(v -> {
                Intent i = new Intent(GuildsActivity.this, CreateGuildActivity.class);
                i.putExtra(EXTRA_USER_ID, safeUserId());
                i.putExtra(EXTRA_EMAIL, safeEmail());
                i.putExtra(EXTRA_USERNAME, safeUsername());
                startActivityForResult(i, 102);  // request code for create
            });
        }

        if (join != null) {
            join.setOnClickListener(v -> {
                Intent i = new Intent(GuildsActivity.this, JoinGuildActivity.class);
                i.putExtra(EXTRA_USER_ID, safeUserId());
                i.putExtra(EXTRA_EMAIL, safeEmail());
                i.putExtra(EXTRA_USERNAME, safeUsername());
                startActivityForResult(i, 101);  // request code for join
            });
        }

        swipe = findViewById(R.id.swipe);
        rv = findViewById(R.id.recycler);
        progress = findViewById(R.id.progress);

        adapter = new GuildAdapter(null);
        adapter.setOnItemClick((guildId, guildName) -> openGuild(guildId, guildName));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        swipe.setOnRefreshListener(this::refreshGuildList);
        refreshGuildList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 101 || requestCode == 102) && resultCode == RESULT_OK) {
            refreshGuildList();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh guild list when returning from join/create
        refreshGuildList();

        // Setup bottom navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.action_bar);
        if (bottomNav != null) {
            setupBottomNavigation(bottomNav);
        }
    }

    private void setupBottomNavigation(com.google.android.material.bottomnavigation.BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_map) {
                Intent i = new Intent(this, cycredit.io.MapActivity.class);
                i.putExtra(EXTRA_USER_ID, safeUserId());
                String em = safeEmail();
                if (!em.isEmpty()) i.putExtra(EXTRA_EMAIL, em);
                String un = safeUsername();
                if (!un.isEmpty()) i.putExtra(EXTRA_USERNAME, un);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            } else if (itemId == R.id.nav_guilds) {
                // Already on Guilds - do nothing
                return true;
            } else if (itemId == R.id.nav_missions) {
                Intent i = new Intent(this, cycredit.io.MissionsActivity.class);
                i.putExtra(EXTRA_USER_ID, safeUserId());
                String em = safeEmail();
                if (!em.isEmpty()) i.putExtra(EXTRA_EMAIL, em);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                Intent i = new Intent(this, cycredit.io.LeaderboardActivity.class);
                i.putExtra(EXTRA_USER_ID, safeUserId());
                String em = safeEmail();
                if (!em.isEmpty()) i.putExtra(EXTRA_EMAIL, em);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            } else if (itemId == R.id.nav_end_turn) {
                // End turn - navigate to map
                Intent i = new Intent(this, cycredit.io.MapActivity.class);
                i.putExtra(EXTRA_USER_ID, safeUserId());
                String em = safeEmail();
                if (!em.isEmpty()) i.putExtra(EXTRA_EMAIL, em);
                String un = safeUsername();
                if (!un.isEmpty()) i.putExtra(EXTRA_USERNAME, un);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_guilds);
    }

    private void refreshGuildList() {
        // Start spinner if SwipeRefreshLayout is present
        if (swipe != null) {
            swipe.setRefreshing(true);
        }
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }

        // Keep existing GuildService.listGuilds call style - DO NOT CHANGE SERVICE
        GuildService.listGuilds(this, new GuildService.ArrayCallback() {
            @Override public void onSuccess(JSONArray arr) {
                if (progress != null) {
                    progress.setVisibility(View.GONE);
                }
                if (swipe != null) {
                    swipe.setRefreshing(false);
                }
                List<Guild> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        list.add(Guild.fromJson(o));
                    }
                }
                // Update adapter exactly how it already does it
                if (adapter != null) {
                    adapter.setItems(list);
                }
            }

            @Override public void onError(com.android.volley.VolleyError error) {
                if (progress != null) {
                    progress.setVisibility(View.GONE);
                }
                if (swipe != null) {
                    swipe.setRefreshing(false);
                }
                android.widget.Toast.makeText(GuildsActivity.this,
                    "Failed to load guilds", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Keep fetchGuilds for backward compatibility if needed elsewhere
    private void fetchGuilds() {
        refreshGuildList();
    }
}