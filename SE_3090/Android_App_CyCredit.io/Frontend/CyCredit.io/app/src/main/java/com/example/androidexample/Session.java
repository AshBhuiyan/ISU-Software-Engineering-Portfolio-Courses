package cycredit.io;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {
    private static final String PREF = "cycredit_session";
    private static final String K_UID = "user_id";
    private static final String K_UN  = "username";

    public static void setUser(Context ctx, int userId, String username){
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putInt(K_UID, userId).putString(K_UN, username).apply();
    }
    public static int getUserId(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getInt(K_UID, -1);
    }
    public static String getUsername(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getString(K_UN, null);
    }
}
