package cycredit.io.guilds;

import android.content.Context;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Central API client for all network requests.
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static RequestQueue queue;
    
    /**
     * Backend server URL - Iowa State class server on port 8080.
     * NOTE: No trailing slash - all endpoints start with "/" (e.g., "/game/state")
     */
    public static String BASE_URL = "http://coms-3090-017.class.las.iastate.edu:8080";
    
    public static synchronized RequestQueue getRequestQueue(Context ctx){
        if (queue == null) {
            queue = Volley.newRequestQueue(ctx.getApplicationContext());
            Log.d(TAG, "=== API CLIENT INITIALIZED ===");
            Log.d(TAG, "BASE_URL: " + BASE_URL);
        }
        return queue;
    }
    
    /**
     * Debug helper - logs the full URL being called.
     * Call this before adding requests to help diagnose network issues.
     * 
     * @param endpoint The endpoint path (e.g., "/game/state?userId=1")
     * @return The full URL for convenience
     */
    public static String logUrl(String endpoint) {
        String fullUrl = BASE_URL + endpoint;
        Log.d(TAG, "[REQUEST] " + fullUrl);
        return fullUrl;
    }
}
