package cycredit.io;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import cycredit.io.Session;

public class MissionsActivity extends AppCompatActivity {

    private int userId;
    private String email;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missions);

        // Get USER_ID as int (same as MapActivity / FinanceHubActivity)
        userId = getIntent().getIntExtra("USER_ID", -1);
        email  = getIntent().getStringExtra("EMAIL");

        ImageButton btnBack = findViewById(R.id.btn_back_missions);
        View cardQuests = findViewById(R.id.card_quests);
        View cardAchievements = findViewById(R.id.card_achievements);

        // Back button: return to previous screen
        btnBack.setOnClickListener(v -> finish());

        // Quests card
        cardQuests.setOnClickListener(v -> {
            Intent i = new Intent(MissionsActivity.this, QuestsActivity.class);
            if (userId != -1) i.putExtra("USER_ID", userId);
            if (email != null) i.putExtra("EMAIL", email);
            startActivity(i);
        });

        // Achievements card
        cardAchievements.setOnClickListener(v -> {
            Intent i = new Intent(MissionsActivity.this, AchievementsActivity.class);
            if (userId != -1) i.putExtra("USER_ID", userId);
            if (email != null) i.putExtra("EMAIL", email);
            startActivity(i);
        });

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
                // Already on Missions - do nothing
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                Intent i = new Intent(this, cycredit.io.LeaderboardActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("EMAIL", email);
                startActivity(i);
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
        bottomNav.setSelectedItemId(R.id.nav_missions);
    }
}
