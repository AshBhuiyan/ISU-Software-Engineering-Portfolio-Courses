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
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestsActivity extends AppCompatActivity {

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;

    private RecyclerView recycler;
    private QuestAdapter adapter;
    private String userId;

    // local cache to find current percent when tapping
    private final Map<String, Integer> progressByQuest = new HashMap<String, Integer>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quests);

        // just use the same numeric USER_ID the rest of the app uses
        int numericId = getIntent().getIntExtra("USER_ID", -1);
        if (numericId != -1) {
            userId = String.valueOf(numericId);
        } else {
            userId = "1";
        }

        recycler = findViewById(R.id.recycler_quests);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new QuestAdapter(
                new ArrayList<QuestModel>(),
                new ArrayList<UserQuestModel>(),
                questId -> bumpProgress(questId, 20) // +20% per tap (demo)
        );
        recycler.setAdapter(adapter);

        // Back button handler
        findViewById(R.id.btn_back_quests).setOnClickListener(v -> finish());

        fetchData();
    }

    private void fetchData() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    // 1) Load quest catalog
                    JSONArray questsJson = getJsonArray(BASE_URL + "/api/quests");
                    final List<QuestModel> quests = new ArrayList<QuestModel>();
                    for (int i = 0; i < questsJson.length(); i++) {
                        JSONObject o = questsJson.getJSONObject(i);
                        QuestModel q = new QuestModel();
                        q.questId = o.optString("questId", null);
                        q.title = o.optString("title", "");
                        q.description = o.optString("description", "");
                        q.rewardPoints = o.optInt("rewardPoints", 0);
                        quests.add(q);
                    }

                    // 2) Load user progress for this userId
                    JSONArray progressJson = getJsonArray(BASE_URL + "/api/quests/progress?userId=" + userId);
                    final List<UserQuestModel> progress = new ArrayList<UserQuestModel>();
                    progressByQuest.clear();
                    for (int i = 0; i < progressJson.length(); i++) {
                        JSONObject o = progressJson.getJSONObject(i);
                        UserQuestModel p = new UserQuestModel();
                        p.questId = o.optString("questId", null);
                        p.status = o.optString("status", "");
                        p.progressPercent = o.optInt("progressPercent", 0);
                        p.completedAtIso = o.optString("completedAtIso", null);
                        progress.add(p);
                        if (p.questId != null) {
                            progressByQuest.put(p.questId, Integer.valueOf(p.progressPercent));
                        }
                    }

                    // 3) Update adapter on UI thread
                    runOnUiThread(new Runnable() {
                        @Override public void run() { adapter.updateData(quests, progress); }
                    });
                } catch (final Exception e) {
                    showError("Failed to load quests: " + e.getMessage());
                }
            }
        }).start();
    }

    private void bumpProgress(final String questId, final int delta) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    int current = progressByQuest.containsKey(questId)
                            ? progressByQuest.get(questId).intValue()
                            : 0;
                    int next = Math.min(100, Math.max(0, current + delta));
                    // POST absolute percent
                    String url = BASE_URL + "/api/quests/" + questId
                            + "/progress?userId=" + userId + "&percent=" + next;
                    int code = post(url);
                    if (code >= 200 && code < 300) {
                        fetchData(); // refresh UI
                    } else {
                        showError("Update failed (HTTP " + code + ")");
                    }
                } catch (Exception e) {
                    showError("Update failed: " + e.getMessage());
                }
            }
        }).start();
    }

    private int post(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(25000);
            conn.setDoOutput(true);
            // trivial body so some servers don't reject empty POST
            byte[] body = "x=1".getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(body.length);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.connect();
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.write(body);
            dos.flush();
            dos.close();
            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                while (br.readLine() != null) {}
                br.close();
            }
            return code;
        } finally {
            if (conn != null) conn.disconnect();
        }
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
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code + " - " + body);
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
                Toast.makeText(QuestsActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Models
    public static class QuestModel {
        public String questId;
        public String title;
        public String description;
        public int rewardPoints;
    }
    public static class UserQuestModel {
        public String questId;
        public String status;
        public int progressPercent;
        public String completedAtIso;
    }
}
