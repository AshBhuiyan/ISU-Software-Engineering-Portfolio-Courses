package cycredit.io;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import cycredit.io.guilds.ApiClient;   // you already have this with BASE_URL

public final class LeaderboardClient {

    private LeaderboardClient() {}

    public static void awardPoints(Context ctx, int userId, String displayName, int delta) {
        if (ctx == null || userId <= 0 || delta == 0) return;

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "User " + userId;
        }

        String encodedName = Uri.encode(displayName);

        String url = ApiClient.BASE_URL
                + "/api/leaderboard/addForUser"
                + "?userId=" + userId
                + "&displayName=" + encodedName
                + "&delta=" + delta;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                (JSONObject) null,
                response -> { /* no-op; server also pushes via WebSocket */ },
                error -> { /* optional: log or toast for debugging */ }
        );

        Volley.newRequestQueue(ctx.getApplicationContext()).add(req);
    }
}
