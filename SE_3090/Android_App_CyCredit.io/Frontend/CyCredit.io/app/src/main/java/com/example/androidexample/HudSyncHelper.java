package cycredit.io;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import cycredit.io.guilds.ApiClient;

/**
 * Helper to broadcast HUD updates after building interactions complete.
 * This is the SINGLE SOURCE OF TRUTH for syncing the HUD across all activities.
 */
public final class HudSyncHelper {

    private static final String TAG = "HudSyncHelper";
    public static final String ACTION_HUD_UPDATE = "cycredit.io.HUD_UPDATE";

    private HudSyncHelper() { }

    /**
     * Refresh HUD from server and broadcast updates to all listening activities.
     * Call this after any action that changes game state (purchases, payments, jobs, etc.)
     */
    public static void refreshHud(Context context, int userId) {
        Log.d(TAG, "=== REFRESHING HUD ===");
        Log.d(TAG, "userId: " + userId);
        
        if (userId <= 0) {
            Log.w(TAG, "Invalid userId, skipping HUD refresh");
            return;
        }
        
        // Use new /game/state endpoint, fallback to /resource if needed
        String url = ApiClient.BASE_URL + "/game/state?userId=" + userId;
        Log.d(TAG, "Fetching from: " + url);
        
        com.android.volley.toolbox.JsonObjectRequest req = new com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.GET,
                url,
                null,
                json -> {
                    try {
                        Log.d(TAG, "Game state response: " + json.toString());
                        
                        int turns = json.optInt("turnsLeft", 0);
                        double money = json.optDouble("money", 0);
                        double credit = json.optDouble("creditScore", json.optDouble("credit", 0));
                        
                        Log.d(TAG, "Broadcasting HUD update: turns=" + turns + 
                            ", money=" + money + ", credit=" + credit);
                        
                        Intent intent = new Intent(ACTION_HUD_UPDATE);
                        intent.putExtra("turnsLeft", turns);
                        intent.putExtra("money", money);
                        intent.putExtra("credit", credit);
                        context.sendBroadcast(intent);
                        
                        Log.d(TAG, "HUD broadcast sent successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing game state", e);
                    }
                },
                error -> {
                    Log.w(TAG, "Game state failed, trying fallback endpoint");
                    // Fallback to old endpoint
                    String fallbackUrl = ApiClient.BASE_URL + "/resource/" + userId;
                    StringRequest fallback = new StringRequest(
                            com.android.volley.Request.Method.GET,
                            fallbackUrl,
                            response -> {
                                try {
                                    Log.d(TAG, "Fallback response: " + response);
                                    JSONObject json = new JSONObject(response);
                                    
                                    int turns = json.optInt("turnsLeft", 0);
                                    double money = json.optDouble("money", 0);
                                    double credit = json.optDouble("credit", 0);
                                    
                                    Log.d(TAG, "Broadcasting HUD update (fallback): turns=" + turns + 
                                        ", money=" + money + ", credit=" + credit);
                                    
                                    Intent intent = new Intent(ACTION_HUD_UPDATE);
                                    intent.putExtra("turnsLeft", turns);
                                    intent.putExtra("money", money);
                                    intent.putExtra("credit", credit);
                                    context.sendBroadcast(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing fallback response", e);
                                }
                            },
                            err -> {
                                Log.e(TAG, "Both endpoints failed for HUD refresh");
                            }
                    );
                    ApiClient.getRequestQueue(context).add(fallback);
                }
        );
        ApiClient.getRequestQueue(context).add(req);
    }
}

