package cycredit.io;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JoinGuildActivity extends AppCompatActivity {

    private int userId;
    private Button btnRefresh, btnAccept;
    private EditText etInviteId;
    private ProgressBar progress;

    private RecyclerView rv;
    private InviteAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_guild);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) userId = cycredit.io.Session.getUserId(this);

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        btnRefresh = findViewById(R.id.btnRefreshInvites);
        btnAccept  = findViewById(R.id.btnAcceptInvite);
        etInviteId = findViewById(R.id.etInviteId);
        progress   = findViewById(R.id.progress);

        rv = findViewById(R.id.recyclerInvites);
        adapter = new InviteAdapter(new InviteAdapter.OnInviteAction() {
            @Override public void onAccept(GuildInvite invite, int pos) {
                if (userId <= 0) { Toast.makeText(JoinGuildActivity.this, "USER_ID missing", Toast.LENGTH_LONG).show(); return; }
                progress.setVisibility(View.VISIBLE);
                GuildService.actOnInvite(JoinGuildActivity.this, invite.id, "accept", userId, new GuildService.JsonCallback() {
                    @Override public void onSuccess(JSONObject json) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(JoinGuildActivity.this, "Joined guild " + invite.guildId, Toast.LENGTH_SHORT).show();
                        adapter.removeRow(pos);
                    }
                    @Override public void onError(VolleyError e) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(JoinGuildActivity.this, "Accept failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onDecline(GuildInvite invite, int pos) {
                if (userId <= 0) { Toast.makeText(JoinGuildActivity.this, "USER_ID missing", Toast.LENGTH_LONG).show(); return; }
                progress.setVisibility(View.VISIBLE);
                GuildService.actOnInvite(JoinGuildActivity.this, invite.id, "decline", userId, new GuildService.JsonCallback() {
                    @Override public void onSuccess(JSONObject json) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(JoinGuildActivity.this, "Declined invite #" + invite.id, Toast.LENGTH_SHORT).show();
                        adapter.removeRow(pos);
                    }
                    @Override public void onError(VolleyError e) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(JoinGuildActivity.this, "Decline failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> {
            if (userId <= 0) { Toast.makeText(this, "USER_ID missing — open from a logged-in screen", Toast.LENGTH_LONG).show(); return; }
            progress.setVisibility(View.VISIBLE);
            fetchInvites();
        });

        btnAccept.setOnClickListener(v -> {
            if (userId <= 0) { Toast.makeText(this, "USER_ID missing — open from a logged-in screen", Toast.LENGTH_LONG).show(); return; }
            String s = etInviteId.getText().toString().trim();
            if (TextUtils.isEmpty(s)) { etInviteId.setError("Enter invite ID"); return; }
            int inviteId;
            try { inviteId = Integer.parseInt(s); }
            catch (NumberFormatException nfe) { etInviteId.setError("Numeric ID"); return; }

            progress.setVisibility(View.VISIBLE);
            GuildService.actOnInvite(this, inviteId, "accept", userId, new GuildService.JsonCallback() {
                @Override public void onSuccess(JSONObject json) {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(JoinGuildActivity.this, "Accepted invite #" + inviteId, Toast.LENGTH_SHORT).show();
                    fetchInvites();
                }
                @Override public void onError(VolleyError e) {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(JoinGuildActivity.this, "Accept failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (userId > 0) fetchInvites();
    }

    private void fetchInvites() {
        GuildService.myInvites(this, userId, new GuildService.ArrayCallback() {
            @Override public void onSuccess(JSONArray arr) {
                progress.setVisibility(View.GONE);
                List<GuildInvite> items = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        GuildInvite gi = new GuildInvite();
                        gi.id = o.optInt("id", 0);
                        gi.guildId = o.optInt("guildId", 0);
                        gi.status = o.optString("status", "pending");
                        if ("pending".equalsIgnoreCase(gi.status)) items.add(gi);
                    }
                }
                adapter.setItems(items);
            }
            @Override public void onError(VolleyError e) { progress.setVisibility(View.GONE); }
        });
    }
}
