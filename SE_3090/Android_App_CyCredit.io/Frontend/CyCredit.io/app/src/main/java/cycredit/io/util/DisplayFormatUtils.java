package cycredit.io.util;

public final class DisplayFormatUtils {

    private DisplayFormatUtils() { }

    public static String formatTurns(int turns) {
        if (turns < 0) {
            return "Turns remaining: 0";
        }
        return "Turns remaining: " + turns;
    }

    public static String formatWellnessProgress(int progress, int target) {
        if (target <= 0) {
            return "0 / 0 completed";
        }
        return progress + " / " + target + " completed";
    }

    public static double calculateStreakBonus(int streak) {
        if (streak <= 0) return 0.0;
        double bonus = Math.min(streak, 3) * 0.1;
        return Math.round(bonus * 100.0) / 100.0;
    }
}

