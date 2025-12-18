package cycredit.io.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class UserPrefs {
    private static final String FILE = "user_prefs";
    private static final String K_ROLE_NAME = "role_name";
    private static final String K_ROLE_ID   = "role_id";
    private static final String K_CHAR_NAME = "character_name";
    private static final String K_CHAR_ID   = "character_id";
    private static final String K_REMEMBER = "remember_me";
    private static final String K_LOGIN_USER = "login_username";
    private static final String K_LOGIN_PASS = "login_password";
    private static final String K_USER_ID = "user_id";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static void saveRole(Context c, String name, int id) {
        sp(c).edit().putString(K_ROLE_NAME, name).putInt(K_ROLE_ID, id).apply();
    }

    public static void saveCharacter(Context c, String name, int id) {
        sp(c).edit().putString(K_CHAR_NAME, name).putInt(K_CHAR_ID, id).apply();
    }

    public static String roleName(Context c) {
        return sp(c).getString(K_ROLE_NAME, "Customer"); // default
    }

    public static int roleId(Context c) {
        return sp(c).getInt(K_ROLE_ID, 51); // your default id
    }

    public static String characterName(Context c) {
        return sp(c).getString(K_CHAR_NAME, "Cy"); // default
    }

    public static int characterId(Context c) {
        return sp(c).getInt(K_CHAR_ID, -1);
    }

    public static void setRememberMe(Context c, boolean remember) {
        sp(c).edit().putBoolean(K_REMEMBER, remember).apply();
    }

    public static boolean rememberMe(Context c) {
        return sp(c).getBoolean(K_REMEMBER, false);
    }

    public static void saveLogin(Context c, String username, String password) {
        sp(c).edit()
            .putString(K_LOGIN_USER, username)
            .putString(K_LOGIN_PASS, password)
            .apply();
    }

    public static String savedUsername(Context c) {
        return sp(c).getString(K_LOGIN_USER, "");
    }

    public static String savedPassword(Context c) {
        return sp(c).getString(K_LOGIN_PASS, "");
    }

    public static void saveUserId(Context c, int userId) {
        sp(c).edit().putInt(K_USER_ID, userId).apply();
    }

    public static int userId(Context c) {
        return sp(c).getInt(K_USER_ID, -1);
    }
}

