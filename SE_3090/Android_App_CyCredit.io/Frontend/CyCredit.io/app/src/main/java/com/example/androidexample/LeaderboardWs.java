package cycredit.io;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class LeaderboardWs {

    public interface Listener {
        void onConnected();
        void onDisconnected(String reason);
        void onError(String message);
        void onLeaderboardUpdate(List<LeaderboardEntry> entries);
    }

    private static final String TAG = "LeaderboardWs";

    private final String url;
    private final Listener listener;
    private final OkHttpClient client = new OkHttpClient();

    private WebSocket socket;

    public LeaderboardWs(String url, Listener listener) {
        this.url = url;
        this.listener = listener;
    }

    public void connect() {
        Request req = new Request.Builder()
                .url(url)
                .build();

        socket = client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                if (listener != null) listener.onConnected();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    // Backend sends a JSON array: [ { rank, userId, displayName, score, updatedAt }, ... ]
                    JSONArray arr = new JSONArray(text);
                    List<LeaderboardEntry> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        LeaderboardEntry e = new LeaderboardEntry();
                        e.rank        = o.optInt("rank", i + 1);
                        e.userId      = o.optString("userId", null);
                        e.displayName = o.optString("displayName", null);
                        e.score       = o.optInt("score", 0);
                        e.updatedAt   = o.optString("updatedAt", null);
                        list.add(e);
                    }
                    if (listener != null) listener.onLeaderboardUpdate(list);
                } catch (Exception ex) {
                    Log.e(TAG, "parse error", ex);
                    if (listener != null) listener.onError("Parse error: " + ex.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "ws failure", t);
                if (listener != null) listener.onError("WebSocket failure: " + t.getMessage());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                if (listener != null) listener.onDisconnected(reason);
            }
        });
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close(1000, "client closing");
            } catch (Exception ignore) {}
        }
    }
}
