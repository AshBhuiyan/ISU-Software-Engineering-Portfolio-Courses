package cycredit.io.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cycredit.io.R;

public class MessageActivity extends AppCompatActivity {

    public static final String EXTRA_SCOPE   = "SCOPE";   // "public" | "guild" | "dm"
    public static final String EXTRA_CHANNEL = "CHANNEL"; // "global" | guildId | dmKey
    public static final String EXTRA_USER_ID = "USER_ID";
    public static final String EXTRA_UNAME   = "USERNAME";

    private String scope;
    private String channel;
    private int userId;
    private String username;

    private MessageWs ws;
    private EditText input;
    private Button sendBtn;
    private ProgressBar spinner;
    private RecyclerView list;
    private MessagesAdapter adapter;
    private TextView title;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        scope   = getIntent().getStringExtra(EXTRA_SCOPE);
        channel = getIntent().getStringExtra(EXTRA_CHANNEL);
        userId  = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        username= getIntent().getStringExtra(EXTRA_UNAME);

        if (TextUtils.isEmpty(scope)) scope = "public";
        if (TextUtils.isEmpty(channel)) channel = "global";

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        title   = findViewById(R.id.tvTitle);
        input   = findViewById(R.id.etMessage);
        sendBtn = findViewById(R.id.btnSend);
        spinner = findViewById(R.id.progress);
        list    = findViewById(R.id.recycler);

        title.setText(scope.toUpperCase() + " / " + channel);
        adapter = new MessagesAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        String baseWs = cycredit.io.guilds.ApiClient.BASE_URL.replace("http://", "ws://").replace("https://","wss://");

        String url;
        switch (scope.toLowerCase()) {
            case "guild":
                url = baseWs + "/ws/chat/guild/" + channel + "/" + (TextUtils.isEmpty(username) ? "user-"+userId : username);
                break;
            case "dm":
                url = baseWs + "/ws/chat/dm/" + channel + "/" + (TextUtils.isEmpty(username) ? "user-"+userId : username);
                break;
            default:
                url = baseWs + "/ws/chat/public/" + channel + "/" + (TextUtils.isEmpty(username) ? "user-"+userId : username);
        }

        ws = new MessageWs(url, new MessageWs.Listener() {
            @Override public void onOpen() {
                runOnUiThread(() -> spinner.setVisibility(View.GONE));
            }
            @Override public void onMessage(String text) {
                try {
                    JSONObject o = new JSONObject(text);
                    ChatRow row = new ChatRow();
                    row.fromUserId = o.optInt("fromUserId", 0);
                    row.username   = o.optString("username","");
                    row.content    = o.optString("content","");
                    row.createdAt  = o.optString("createdAt","");
                    runOnUiThread(() -> adapter.add(row));
                } catch (Exception ignored){}
            }
            @Override public void onClosed(int code, String reason) {
                runOnUiThread(() -> Toast.makeText(MessageActivity.this,"Socket closed",Toast.LENGTH_SHORT).show());
            }
            @Override public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MessageActivity.this,"Socket error: "+t.getMessage(),Toast.LENGTH_LONG).show());
            }
        });
        spinner.setVisibility(View.VISIBLE);
        ws.connect();

        sendBtn.setOnClickListener(v -> {
            String body = input.getText().toString().trim();
            if (TextUtils.isEmpty(body)) return;
            JSONObject msg = new JSONObject();
            try {
                msg.put("fromUserId", userId);
                if (!TextUtils.isEmpty(username)) msg.put("username", username);
                msg.put("content", body);
            } catch (Exception ignored){}
            ws.send(msg.toString());
            input.setText("");
        });
    }

    @Override protected void onDestroy() {
        if (ws != null) ws.close();
        super.onDestroy();
    }

    /* ----------------- simple view models / adapter ------------------ */

    public static class ChatRow {
        public int fromUserId;
        public String username;
        public String content;
        public String createdAt;
    }

    static class MessagesAdapter extends RecyclerView.Adapter<MessageVH> {
        private final List<ChatRow> data = new ArrayList<>();
        void add(ChatRow r){ data.add(r); notifyItemInserted(data.size()-1); }
        @Override public MessageVH onCreateViewHolder(android.view.ViewGroup p, int v) {
            android.view.View view = android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_message, p, false);
            return new MessageVH(view);
        }
        @Override public void onBindViewHolder(MessageVH h, int pos) {
            ChatRow r = data.get(pos);
            String who = TextUtils.isEmpty(r.username) ? ("ID "+r.fromUserId) : (r.username + " (ID "+r.fromUserId+")");
            h.title.setText(who);
            h.body.setText(r.content);
            h.time.setText(TextUtils.isEmpty(r.createdAt) ? "" : r.createdAt);
        }
        @Override public int getItemCount(){ return data.size(); }
    }

    static class MessageVH extends RecyclerView.ViewHolder {
        TextView title, body, time;
        MessageVH(View v){
            super(v);
            title = v.findViewById(R.id.tvWho);
            body  = v.findViewById(R.id.tvBody);
            time  = v.findViewById(R.id.tvTime);
        }
    }
}
