package cycredit.io;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import cycredit.io.guilds.ApiClient;

public class GuildService {
    private static final String TAG = "GuildService";
    public static final String BASE_URL = ApiClient.BASE_URL;

    /* ------------ helpers ------------ */
    private static Map<String,String> jsonHeaders() {
        Map<String,String> h = new HashMap<>();
        h.put("Accept", "application/json");
        h.put("Content-Type", "application/json; charset=utf-8");
        return h;
    }
    private static RetryPolicy timeout() {
        return new RetryPolicy() {
            @Override public int getCurrentTimeout() { return 10000; }
            @Override public int getCurrentRetryCount() { return 0; }
            @Override public void retry(VolleyError error) {}
        };
    }
    private static void toastVolley(Context ctx, String op, VolleyError e) {
        String msg = op + " failed";
        NetworkResponse nr = e.networkResponse;
        if (nr != null) {
            String body = "";
            try { body = new String(nr.data, StandardCharsets.UTF_8); } catch (Exception ignored) {}
            msg += " (" + nr.statusCode + ")";
            if (!TextUtils.isEmpty(body)) msg += ": " + body;
        } else if (e.getMessage() != null) {
            msg += ": " + e.getMessage();
        }
        Log.e(TAG, msg, e);
        Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
    private static String url(String path){ return BASE_URL + path; }

    /* ------------ callbacks ------------ */
    public interface ArrayCallback { void onSuccess(JSONArray arr); void onError(VolleyError e); }
    public interface JsonCallback  { void onSuccess(JSONObject json); void onError(VolleyError e); }
    public interface NameCallback  { void onSuccess(String username); void onError(VolleyError e); }

    /* ------------ guild list/create ------------ */
    public static void listGuilds(Context ctx, final ArrayCallback cb) {
        String u = url("/guilds");
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, u, null,
                cb::onSuccess,
                e -> { toastVolley(ctx, "List guilds", e); cb.onError(e); }) {
            @Override public Map<String, String> getHeaders() { return jsonHeaders(); }
        };
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    public static void createGuild(Context ctx, String name, int creatorUserId, final JsonCallback cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("creatorUserId", creatorUserId);
        } catch (Exception ignored) {}
        String u = url("/guilds");
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, u, body,
                cb::onSuccess,
                e -> { toastVolley(ctx, "Create guild", e); cb.onError(e); }) {
            @Override public Map<String, String> getHeaders() { return jsonHeaders(); }
        };
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    /* ------------ members ------------ */
    public static void getMembers(Context ctx, int guildId, final ArrayCallback cb) {
        String u = url("/guilds/" + guildId + "/members");
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, u, null,
                cb::onSuccess,
                e -> { toastVolley(ctx, "Get members", e); cb.onError(e); }) {
            @Override public Map<String, String> getHeaders() { return jsonHeaders(); }
        };
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    /* ------------ invites (read/act) ------------ */
    public static void myInvites(Context ctx, int receiverUserId, final ArrayCallback cb) {
        // aligned with backend route
        String u = url("/guilds/me/invites?receiverUserId=" + receiverUserId);
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, u, null,
                cb::onSuccess,
                e -> { toastVolley(ctx, "My invites", e); cb.onError(e); }) {
            @Override public Map<String, String> getHeaders() { return jsonHeaders(); }
        };
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    public static void actOnInvite(Context ctx, int inviteId, String action, int receiverUserId, final JsonCallback cb) {
        String u = url("/invites/" + inviteId + "/" + action + "?receiverUserId=" + receiverUserId);
        StringRequest req = new StringRequest(Request.Method.POST, u,
                resp -> {
                    JSONObject out = new JSONObject();
                    try { out.put("message", (resp == null || resp.isEmpty()) ? "OK" : resp); } catch (Exception ignored) {}
                    cb.onSuccess(out);
                },
                e -> { toastVolley(ctx, "Invite " + action, e); cb.onError(e); }
        );
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    /* ------------ invite send (ID / handle/email / username) ------------ */


    public static void inviteByUserId(Context ctx, int guildId, int senderUserId, int receiverUserId, final JsonCallback cb) {
        createInviteById(ctx, guildId, senderUserId, receiverUserId, cb);
    }


    public static void inviteByUsername(Context ctx, int guildId, int senderUserId, String username, final JsonCallback cb) {
        final String handle = (username == null) ? "" : username.trim();
        if (handle.isEmpty()) { cb.onError(new VolleyError("empty username")); return; }

        final String u = url("/guilds/" + guildId + "/invites/by-name");

        JSONObject body = new JSONObject();
        try {
            body.put("senderUserId", senderUserId);  // <- now in BODY
            body.put("receiverName", handle);        // <- now in BODY
        } catch (Exception ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, u, body,
                cb::onSuccess,
                e -> { toastVolley(ctx, "Invite by username", e); cb.onError(e); }) {
            @Override public Map<String, String> getHeaders() { return jsonHeaders(); }
        };
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    // by userId
    public static void createInviteById(Context ctx, int guildId, int senderUserId, int receiverUserId, final JsonCallback cb) {
        final String[] urls = new String[] {
                url("/guilds/" + guildId + "/invites?senderUserId=" + senderUserId + "&receiverUserId=" + receiverUserId),
                url("/invites?guildId=" + guildId + "&senderUserId=" + senderUserId + "&receiverUserId=" + receiverUserId)
        };
        postEmptyJsonTry(ctx, urls, 0, "Create invite (by id)", cb);
    }


    public static void inviteByHandleOrEmail(Context ctx, int guildId, int senderUserId, String input, final JsonCallback cb) {
        if (TextUtils.isEmpty(input)) { cb.onError(new VolleyError("empty username/email")); return; }
        final String trimmed = input.trim();

        if (TextUtils.isDigitsOnly(trimmed)) {
            try {
                int receiverId = Integer.parseInt(trimmed);
                inviteByUserId(ctx, guildId, senderUserId, receiverId, cb);
                return;
            } catch (NumberFormatException nfe) {
                cb.onError(new VolleyError("invalid user id"));
                return;
            }
        }

        if (trimmed.contains("@")) {
            inviteByEmail(ctx, guildId, senderUserId, trimmed, cb);
            return;
        }

        inviteByUsername(ctx, guildId, senderUserId, trimmed, cb);
    }

    // email path
    public static void inviteByEmail(Context ctx, int guildId, int senderUserId, String email, final JsonCallback cb) {
        final String emailFinal = email.trim();
        String enc;
        try { enc = URLEncoder.encode(emailFinal, StandardCharsets.UTF_8.toString()); }
        catch (Exception ex) { enc = emailFinal; }
        final String encFinal = enc;

        final String[] urlsPref = new String[] {
                url("/guilds/" + guildId + "/invites?senderUserId=" + senderUserId + "&emailId=" + encFinal),
                url("/invites?guildId=" + guildId + "&senderUserId=" + senderUserId + "&emailId=" + encFinal)
        };
        postEmptyJsonTry(ctx, urlsPref, 0, "Create invite (by emailId)", new JsonCallback() {
            @Override public void onSuccess(JSONObject json) { cb.onSuccess(json); }
            @Override public void onError(VolleyError e) {
                final String[] urlsAlt = new String[] {
                        url("/guilds/" + guildId + "/invites?senderUserId=" + senderUserId + "&email=" + encFinal),
                        url("/invites?guildId=" + guildId + "&senderUserId=" + senderUserId + "&email=" + encFinal)
                };
                postEmptyJsonTry(ctx, urlsAlt, 0, "Create invite (by email)", cb);
            }
        });
    }


    private static void postEmptyJsonTry(Context ctx, final String[] urls, final int idx, final String op, final JsonCallback cb) {
        if (idx >= urls.length) { cb.onError(new VolleyError("No endpoint for " + op)); return; }
        String u = urls[idx];
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, u, new JSONObject(),
                cb::onSuccess,
                e -> {
                    if (e.networkResponse != null && e.networkResponse.statusCode == 404) {
                        postEmptyJsonTry(ctx, urls, idx + 1, op, cb);
                    } else {
                        toastVolley(ctx, op, e);
                        cb.onError(e);
                    }
                }) {
            @Override public Map<String, String> getHeaders() { return jsonHeaders(); }
        };
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }



    public static void getUsernameById(Context ctx, int userId, final NameCallback cb) {
        String[] tries = new String[] {
                "/users/" + userId,
                "/users?id=" + userId,
                "/user/" + userId,
                "/user?id=" + userId
        };
        tryGetUser(ctx, tries, 0, userId, cb);
    }

    private static void tryGetUser(Context ctx, String[] paths, int i, int userId, final NameCallback cb) {
        if (i >= paths.length) { cb.onError(new VolleyError("User not found")); return; }
        String u = url(paths[i]);
        StringRequest req = new StringRequest(Request.Method.GET, u,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        String name = o.optString("name");
                        if (TextUtils.isEmpty(name)) name = o.optString("username");
                        if (TextUtils.isEmpty(name)) name = o.optString("emailId");
                        if (TextUtils.isEmpty(name)) name = "ID " + userId;
                        cb.onSuccess(name);
                    } catch (Exception ex) {
                        cb.onError(new VolleyError("Parse error: " + ex.getMessage()));
                    }
                },
                e -> {
                    if (e.networkResponse != null &&
                            (e.networkResponse.statusCode == 404 || e.networkResponse.statusCode == 400)) {
                        tryGetUser(ctx, paths, i + 1, userId, cb);
                    } else {
                        cb.onError(e);
                    }
                });
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }

    public static void getUserByHandle(Context ctx, String handle, final JsonCallback cb) {
        final String h = handle.trim();
        final String[] urls = new String[] {
                url("/users?name=" + h),
                url("/users?username=" + h),
                url("/users?emailId=" + h)
        };
        getFirstJsonTry(ctx, urls, 0, cb);
    }

    private static void getFirstJsonTry(Context ctx, final String[] urls, final int idx, final JsonCallback cb) {
        if (idx >= urls.length) { cb.onError(new VolleyError("User lookup failed")); return; }
        StringRequest req = new StringRequest(Request.Method.GET, urls[idx],
                resp -> {
                    try { cb.onSuccess(new JSONObject(resp)); }
                    catch (Exception ex) { cb.onError(new VolleyError("Parse error: " + ex.getMessage())); }
                },
                e -> {
                    if (e.networkResponse != null && e.networkResponse.statusCode == 404) {
                        getFirstJsonTry(ctx, urls, idx + 1, cb);
                    } else {
                        cb.onError(e);
                    }
                });
        req.setRetryPolicy(timeout());
        ApiClient.getRequestQueue(ctx).add(req);
    }
}
