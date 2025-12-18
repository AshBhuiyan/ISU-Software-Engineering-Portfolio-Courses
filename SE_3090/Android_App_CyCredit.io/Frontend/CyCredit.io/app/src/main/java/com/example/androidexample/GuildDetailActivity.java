package cycredit.io.guilds;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cycredit.io.GuildService;
import cycredit.io.MemberAdapter;
import cycredit.io.R;

public class GuildDetailActivity extends AppCompatActivity {

    private static final String TAG = "GuildDetail";

    private int guildId;
    private int userId;

    private TextView tvName, tvCount;
    private ProgressBar progress;
    private RecyclerView rvMembers;
    private MemberAdapter memberAdapter;

    private EditText etReceiverUserId, etReceiverHandle;
    private Button btnInviteById, btnInviteByHandle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guild_detail);

        // Defensive extras reading - use constants from GuildsActivity
        guildId = getIntent().getIntExtra(GuildsActivity.EXTRA_GUILD_ID, -1);
        if (guildId <= 0) {
            // Fallback to old key for backward compatibility
            guildId = getIntent().getIntExtra("guildId", -1);
        }
        if (guildId <= 0) {
            android.widget.Toast.makeText(this, "Missing guild id", android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userId = getIntent().getIntExtra(GuildsActivity.EXTRA_USER_ID, -1);
        if (userId <= 0) userId = cycredit.io.Session.getUserId(this);

        String guildName = getIntent().getStringExtra(GuildsActivity.EXTRA_GUILD_NAME);
        if (guildName == null) {
            // Fallback to old key for backward compatibility
            guildName = getIntent().getStringExtra("GUILD_NAME");
        }

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        ImageButton chat = findViewById(R.id.btnGuildChat);
        chat.setOnClickListener(v -> {
            String uname = getIntent().getStringExtra(GuildsActivity.EXTRA_USERNAME);
            if (uname == null || uname.isEmpty()) {
                uname = cycredit.io.Session.getUsername(this);
            }
            Intent chatIntent = new Intent(this, cycredit.io.chat.MessageActivity.class);
            chatIntent.putExtra("SCOPE", "guild");
            chatIntent.putExtra("CHANNEL", String.valueOf(guildId));
            chatIntent.putExtra("USER_ID", userId);
            chatIntent.putExtra("USERNAME", (uname == null) ? ("user-" + userId) : uname);
            startActivity(chatIntent);
        });

        tvName   = findViewById(R.id.tvGuildName);
        tvCount  = findViewById(R.id.tvMemberCount);
        progress = findViewById(R.id.progress);
        rvMembers = findViewById(R.id.recyclerMembers);

        if (!TextUtils.isEmpty(guildName)) tvName.setText(guildName);

        btnInviteById     = findViewById(R.id.btnInviteById);
        btnInviteByHandle = findViewById(R.id.btnInviteByUsername);
        etReceiverUserId  = findViewById(R.id.etReceiverUserId);
        etReceiverHandle  = findViewById(R.id.etReceiverUsername);

        memberAdapter = new MemberAdapter();
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setNestedScrollingEnabled(false);
        rvMembers.setAdapter(memberAdapter);

        btnInviteById.setOnClickListener(v -> {
            String rx = etReceiverUserId.getText().toString().trim();
            if (TextUtils.isEmpty(rx)) { etReceiverUserId.setError("Receiver userId"); return; }
            int receiverId;
            try { receiverId = Integer.parseInt(rx); }
            catch (NumberFormatException nfe) { etReceiverUserId.setError("Numeric ID"); return; }

            progress.setVisibility(android.view.View.VISIBLE);
            GuildService.createInviteById(this, guildId, userId, receiverId, new GuildService.JsonCallback() {
                @Override public void onSuccess(JSONObject json) {
                    progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(GuildDetailActivity.this, "Invite sent to userId=" + receiverId, Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(VolleyError e) {
                    progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(GuildDetailActivity.this, "Create invite failed", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnInviteByHandle.setOnClickListener(v -> {
            String input = etReceiverHandle.getText().toString().trim();
            if (TextUtils.isEmpty(input)) { etReceiverHandle.setError("Enter username, email, or user ID"); return; }

            progress.setVisibility(android.view.View.VISIBLE);
            GuildService.inviteByHandleOrEmail(this, guildId, userId, input, new GuildService.JsonCallback() {
                @Override public void onSuccess(JSONObject json) {
                    progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(GuildDetailActivity.this, "Invite sent to " + input, Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(VolleyError e) {
                    progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(GuildDetailActivity.this, "Create invite failed", Toast.LENGTH_SHORT).show();
                }
            });
        });

        fetchMembers();
    }

    @Override protected void onResume() {
        super.onResume();
        fetchMembers();
    }

    private void fetchMembers() {
        progress.setVisibility(android.view.View.VISIBLE);
        GuildService.getMembers(this, guildId, new GuildService.ArrayCallback() {
            @Override public void onSuccess(JSONArray arr) {
                progress.setVisibility(android.view.View.GONE);

                List<GuildMember> list = new ArrayList<>();
                int len = (arr == null) ? 0 : arr.length();
                tvCount.setText(String.valueOf(len));

                if (arr != null) {
                    for (int i = 0; i < len; i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;

                        Log.d(TAG, "member payload: " + o);

                        GuildMember m = new GuildMember();
                        m.id = o.optInt("userId", 0);

                        // Use whatever key exists; leave blank if absent so we can resolve it.
                        String uname = o.optString("name", "");
                        if (TextUtils.isEmpty(uname)) uname = o.optString("username", "");
                        if (TextUtils.isEmpty(uname)) uname = o.optString("userName", "");
                        if (TextUtils.isEmpty(uname)) uname = o.optString("displayName", "");
                        m.username = uname;
                        m.role = o.optString("role", "MEMBER");

                        list.add(m);
                    }
                }

                memberAdapter.setItems(list);

                // **Important**: resolve missing usernames after list is set.
                resolveUsernamesInPlace(list);
            }
            @Override public void onError(VolleyError e) {
                progress.setVisibility(android.view.View.GONE);
            }
        });
    }

    /** For any member where username is missing, fetch it via a user endpoint and update the row. */
    private void resolveUsernamesInPlace(List<GuildMember> list) {
        if (list == null || list.isEmpty()) return;

        for (int idx = 0; idx < list.size(); idx++) {
            final int pos = idx;
            GuildMember m = list.get(pos);
            if (!TextUtils.isEmpty(m.username)) continue; // already has a name

            fetchUsernameFlexible(m.id, name -> {
                if (!TextUtils.isEmpty(name)) {
                    m.username = name;
                    // update only that row
                    runOnUiThread(() -> memberAdapter.notifyItemChanged(pos));
                }
            });
        }
    }

    /* ---------------- username lookup helpers ---------------- */

    interface NameCb { void onName(String name); }

    private void fetchUsernameFlexible(int userId, NameCb cb) {
        // Try a few likely user endpoints (adjust these to your real ones if you know them!)
        String base = ApiClient.BASE_URL;
        String[] tries = new String[]{
                base + "/users/" + userId,
                base + "/user/" + userId,
                base + "/users?id=" + userId,
                base + "/user?id=" + userId,
                base + "/api/users/" + userId
        };
        fetchTry(tries, 0, userId, cb);
    }

    private void fetchTry(String[] urls, int idx, int userId, NameCb cb) {
        if (idx >= urls.length) { cb.onName(null); return; }
        String u = urls[idx];

        StringRequest req = new StringRequest(
                Request.Method.GET, u,
                resp -> {
                    String name = parseUsernameFlexible(resp);
                    if (TextUtils.isEmpty(name)) {
                        fetchTry(urls, idx + 1, userId, cb);
                    } else {
                        cb.onName(name);
                    }
                },
                err -> {
                    int code = (err.networkResponse == null) ? -1 : err.networkResponse.statusCode;
                    Log.w(TAG, "username GET fail " + code + " at " + u);
                    if (code == 400 || code == 404) {
                        fetchTry(urls, idx + 1, userId, cb);
                    } else {
                        cb.onName(null);
                    }
                }
        );
        ApiClient.getRequestQueue(this).add(req);
    }

    private String parseUsernameFlexible(String resp) {
        if (resp == null) return null;

        try {
            JSONObject o = new JSONObject(resp);
            if (o.has("data") && o.opt("data") instanceof JSONObject) {
                o = o.getJSONObject("data");
            }
            String name = o.optString("name", "");
            if (name.isEmpty()) name = o.optString("username", "");
            if (name.isEmpty()) name = o.optString("userName", "");
            if (!name.isEmpty()) return name;

            if (o.has("user") && o.opt("user") instanceof JSONObject) {
                JSONObject u = o.getJSONObject("user");
                name = u.optString("name", "");
                if (name.isEmpty()) name = u.optString("username", "");
                if (name.isEmpty()) name = u.optString("userName", "");
                if (!name.isEmpty()) return name;
            }
        } catch (Exception ignore) {}

        try {
            JSONArray arr = new JSONArray(resp);
            if (arr.length() > 0) {
                JSONObject o = arr.getJSONObject(0);
                String name = o.optString("name", "");
                if (name.isEmpty()) name = o.optString("username", "");
                if (name.isEmpty()) name = o.optString("userName", "");
                if (!name.isEmpty()) return name;
            }
        } catch (Exception ignore2) {}

        String t = resp.trim();
        if (!t.isEmpty() && !t.startsWith("{") && !t.startsWith("[")) {
            return t.replace("\"", "");
        }
        return null;
    }
}
