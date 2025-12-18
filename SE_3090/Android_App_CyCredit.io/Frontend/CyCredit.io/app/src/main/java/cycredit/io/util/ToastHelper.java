package cycredit.io.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Utility class for consistent toast messages for CRUD operations
 */
public class ToastHelper {
    
    public static void showCreate(Context context, String resource) {
        Toast.makeText(context, "✓ Created: " + resource, Toast.LENGTH_SHORT).show();
    }
    
    public static void showRead(Context context, String resource, int count) {
        if (count > 0) {
            Toast.makeText(context, "✓ Fetched " + count + " " + resource, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "✓ No " + resource + " found", Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void showUpdate(Context context, String resource) {
        Toast.makeText(context, "✓ Updated: " + resource, Toast.LENGTH_SHORT).show();
    }
    
    public static void showDelete(Context context, String resource) {
        Toast.makeText(context, "✓ Deleted: " + resource, Toast.LENGTH_SHORT).show();
    }
    
    public static void showList(Context context, String resource, int count) {
        Toast.makeText(context, "✓ Loaded " + count + " " + resource, Toast.LENGTH_SHORT).show();
    }
    
    public static void showError(Context context, String operation, String error) {
        Toast.makeText(context, "✗ " + operation + " failed: " + error, Toast.LENGTH_LONG).show();
    }
    
    public static void showSuccess(Context context, String message) {
        Toast.makeText(context, "✓ " + message, Toast.LENGTH_SHORT).show();
    }
    
    public static void showInfo(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

