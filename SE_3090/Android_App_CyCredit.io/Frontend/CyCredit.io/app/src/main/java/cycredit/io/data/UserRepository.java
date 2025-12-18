package cycredit.io.data;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public final class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String BASE = cycredit.io.guilds.ApiClient.BASE_URL;

    public interface Callback {
        void onDone();
    }

    public static void refreshUserFromServer(Context ctx, int userId, Callback cb) {
        if (userId <= 0) {
            if (cb != null) cb.onDone();
            return;
        }
        
        // Try path variable first: /users/{userId}
        String url1 = BASE + "/users/" + userId;
        StringRequest req1 = new StringRequest(
                Request.Method.GET,
                url1,
                resp -> {
                    if (!parseAndPersist(ctx, resp)) {
                        // Fallback: query parameter /user/id?id={userId}
                        String url2 = BASE + "/user/id?id=" + userId;
                        StringRequest req2 = new StringRequest(
                                Request.Method.GET,
                                url2,
                                resp2 -> {
                                    parseAndPersist(ctx, resp2);
                                    if (cb != null) cb.onDone();
                                },
                                err2 -> {
                                    Log.w(TAG, "GET /user/id failed: " + err2);
                                    if (cb != null) cb.onDone();
                                }
                        );
                        Volley.newRequestQueue(ctx).add(req2);
                    } else {
                        if (cb != null) cb.onDone();
                    }
                },
                err -> {
                    Log.w(TAG, "GET /users/{id} failed: " + err);
                    // Try fallback
                    String url2 = BASE + "/user/id?id=" + userId;
                    StringRequest req2 = new StringRequest(
                            Request.Method.GET,
                            url2,
                            resp2 -> {
                                parseAndPersist(ctx, resp2);
                                if (cb != null) cb.onDone();
                            },
                            err2 -> {
                                Log.w(TAG, "GET /user/id failed: " + err2);
                                if (cb != null) cb.onDone();
                            }
                    );
                    Volley.newRequestQueue(ctx).add(req2);
                }
        );
        Volley.newRequestQueue(ctx).add(req1);
    }

    private static boolean parseAndPersist(Context ctx, String json) {
        try {
            JSONObject o = new JSONObject(json);
            Integer id = null;
            
            if (o.has("id")) {
                id = o.getInt("id");
            }
            
            // Fallback: if nested
            if (id == null && o.has("user")) {
                JSONObject u = o.getJSONObject("user");
                if (u.has("id")) {
                    id = u.getInt("id");
                }
            }
            
            if (id != null && id > 0) {
                cycredit.io.util.UserPrefs.saveUserId(ctx, id);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "parse failed", e);
        }
        return false;
    }
}

