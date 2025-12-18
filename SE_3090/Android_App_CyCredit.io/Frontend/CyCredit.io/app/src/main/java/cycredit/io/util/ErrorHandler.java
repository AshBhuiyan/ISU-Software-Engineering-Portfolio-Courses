package cycredit.io.util;

import android.content.Context;
import android.widget.Toast;
import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for handling API error responses consistently across activities.
 */
public class ErrorHandler {

    public static void handleError(Context context, VolleyError error, String defaultMessage) {
        String message = defaultMessage;
        String errorCode = null;

        NetworkResponse response = error.networkResponse;
        if (response != null && response.data != null) {
            try {
                String body = new String(response.data, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);
                errorCode = json.optString("error", null);
                message = json.optString("message", defaultMessage);
            } catch (Exception e) {
                // Fall back to default message
            }
        }

        // Show user-friendly messages based on error code
        if (errorCode != null) {
            switch (errorCode) {
                case "NO_TURNS":
                    message = "You're out of turns for this month. End the month to continue.";
                    break;
                case "INSUFFICIENT_FUNDS":
                    message = "Insufficient funds. Check your balance.";
                    break;
                case "ROOM_CAP_REACHED":
                    message = "Room cap reached (20 items). Remove items to place more.";
                    break;
                case "DUPLICATE_NONCE":
                    message = "This action has already been processed.";
                    break;
                case "INVALID_DURATION":
                    message = "Run duration too short. Please try again.";
                    break;
                case "SOFT_CAP":
                    message = "Soft cap applied - payout reduced due to high activity.";
                    break;
                case "ALREADY_MASTERED":
                    message = "You've already mastered this question.";
                    break;
                case "NOT_COMPLETED":
                    message = "Challenge not completed yet.";
                    break;
                case "PAYMENT_TOO_LOW":
                    message = "Payment must be at least the minimum due.";
                    break;
                case "PAYMENT_TOO_HIGH":
                    message = "Payment cannot exceed total due.";
                    break;
                case "OUT_OF_CREDIT":
                    message = "Purchase would exceed credit limit.";
                    break;
                case "RESOURCE_NOT_FOUND":
                    message = "Resource not found. Please try again.";
                    break;
                case "STATEMENT_NOT_FOUND":
                    message = "Statement not found.";
                    break;
                case "BELOW_MINIMUM":
                    message = "Payment must be at least the minimum due.";
                    break;
                case "UNAUTHORIZED":
                    message = "You don't have permission for this action.";
                    break;
                default:
                    // Use the message from server or default
                    break;
            }
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static String extractErrorCode(VolleyError error) {
        NetworkResponse response = error.networkResponse;
        if (response != null && response.data != null) {
            try {
                String body = new String(response.data, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);
                return json.optString("error", null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static String extractErrorMessage(VolleyError error) {
        NetworkResponse response = error.networkResponse;
        if (response != null && response.data != null) {
            try {
                String body = new String(response.data, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);
                return json.optString("message", error.getMessage());
            } catch (Exception e) {
                return error.getMessage();
            }
        }
        return error.getMessage();
    }
    
    public static String getErrorMessage(VolleyError error) {
        return extractErrorMessage(error);
    }
}

