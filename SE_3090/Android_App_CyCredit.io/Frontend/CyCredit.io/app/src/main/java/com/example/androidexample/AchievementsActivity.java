package cycredit.io;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads achievements using HttpURLConnection (no ApiClient, no OkHttp).
 * Expects backend endpoint: GET /api/achievements?userId=...
 */
public class AchievementsActivity extends AppCompatActivity {

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;

    private RecyclerView recycler;
    private AchievementAdapter adapter;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        String userIdStr = getIntent().getStringExtra("USER_ID");
        if (userIdStr == null) {
            userIdStr = String.valueOf(getIntent().getIntExtra("USER_ID", 1));
        }
        userId = userIdStr;

        recycler = findViewById(R.id.recycler_achievements);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AchievementAdapter(new ArrayList<AchievementModel>());
        recycler.setAdapter(adapter);

        // Back button handler
        findViewById(R.id.btn_back_achievements).setOnClickListener(v -> finish());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Achievements");
        }

        fetchData();
    }

    private void fetchData() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    // Use correct endpoint: /achievements/me?userId=X
                    int userIdInt = Integer.parseInt(userId);
                    JSONArray arr = getJsonArray(BASE_URL + "/achievements/me?userId=" + userIdInt);
                    final List<AchievementModel> items = new ArrayList<AchievementModel>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        AchievementModel a = new AchievementModel();
                        a.title = o.optString("title", "");
                        a.subtitle = o.optString("description", ""); // Backend uses "description"
                        a.achievedAtIso = o.optString("unlockedAt", null); // Backend uses "unlockedAt"
                        a.points = 0; // Backend doesn't return points in this endpoint
                        items.add(a);
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            adapter.updateData(items);
                            cycredit.io.util.ToastHelper.showList(AchievementsActivity.this, "achievements", items.size());
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            showError("Failed to load achievements: " + e.getMessage());
                            cycredit.io.util.ToastHelper.showError(AchievementsActivity.this, "Fetch achievements", e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private JSONArray getJsonArray(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(25000);
            conn.connect();

            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = streamToString(is);
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + " - " + body);
            }
            return new JSONArray(body);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String streamToString(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private void showError(final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(AchievementsActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static class AchievementModel {
        public String title;
        public String subtitle;
        public String achievedAtIso;
        public int points;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // or finish();
        return true;
    }

}
