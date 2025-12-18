package cycredit.io;

import android.util.LruCache;

public class UsernameCache {
    private static final LruCache<Integer, String> CACHE = new LruCache<>(256);

    public static String get(int id) { return CACHE.get(id); }
    public static void put(int id, String name) {
        if (id > 0 && name != null && !name.trim().isEmpty()) CACHE.put(id, name.trim());
    }
}
