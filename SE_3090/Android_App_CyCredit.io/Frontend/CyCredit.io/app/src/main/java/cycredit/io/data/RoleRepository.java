package cycredit.io.data;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public final class RoleRepository {
    private static final String TAG = "RoleRepository";
    private static final String BASE = cycredit.io.guilds.ApiClient.BASE_URL;

    public interface Callback {
        void onDone();
    }

    // Try a couple common endpoints to read the user's role.
    // Adjust if your app already has a single known read endpoint.
    public static void refreshRoleFromServer(Context ctx, int userId, Callback cb) {
        // 1) Try /users/{userId}
        String url1 = BASE + "/users/" + userId;
        StringRequest r1 = new StringRequest(
                Request.Method.GET,
                url1,
                resp -> {
                    if (!parseAndPersist(ctx, resp)) {
                        // 2) Fallback: /role/{userId}
                        String url2 = BASE + "/role/" + userId;
                        StringRequest r2 = new StringRequest(
                                Request.Method.GET,
                                url2,
                                resp2 -> { parseAndPersist(ctx, resp2); if (cb != null) cb.onDone(); },
                                err2 -> { Log.w(TAG, "role GET fallback failed: " + err2); if (cb != null) cb.onDone(); }
                        );
                        Volley.newRequestQueue(ctx).add(r2);
                    } else {
                        if (cb != null) cb.onDone();
                    }
                },
                err -> {
                    Log.w(TAG, "user GET failed: " + err);
                    // Try the fallback anyway
                    String url2 = BASE + "/role/" + userId;
                    StringRequest r2 = new StringRequest(
                            Request.Method.GET,
                            url2,
                            resp2 -> { parseAndPersist(ctx, resp2); if (cb != null) cb.onDone(); },
                            err2 -> { Log.w(TAG, "role GET fallback failed: " + err2); if (cb != null) cb.onDone(); }
                    );
                    Volley.newRequestQueue(ctx).add(r2);
                }
        );
        Volley.newRequestQueue(ctx).add(r1);
    }

    // Accepts either a user payload with nested role or a role payload; persists server truth.
    private static boolean parseAndPersist(Context ctx, String json) {
        try {
            JSONObject o = new JSONObject(json);
            Integer roleId = null;
            String roleName = null;

            // Common shapes:
            // { "id": 123, "roleId": 51, "roleName":"Customer", ... }
            if (o.has("roleId")) roleId = o.getInt("roleId");
            if (o.has("roleName")) roleName = o.getString("roleName");

            // nested: { "role": { "id": 51, "name":"Customer" }, ... }
            if (o.has("role")) {
                JSONObject r = o.getJSONObject("role");
                if (r.has("id")) roleId = r.getInt("id");
                if (r.has("name")) roleName = r.getString("name");
            }

            // In case the role is a simple object response: { "id": 51, "name":"Customer" }
            if (roleId == null && o.has("id") && o.has("name")) {
                roleId = o.getInt("id");
                roleName = o.getString("name");
            }

            if (roleName != null && roleId != null) {
                cycredit.io.util.UserPrefs.saveRole(ctx, roleName, roleId);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "parse failed", e);
        }
        return false;
    }
}

