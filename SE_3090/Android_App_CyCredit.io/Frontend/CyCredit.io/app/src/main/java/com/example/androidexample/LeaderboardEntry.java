package cycredit.io;

public class LeaderboardEntry {

    public int rank;
    public String userId;
    public String displayName;
    public int score;
    public String updatedAt; // ISO-8601 string from backend

    public LeaderboardEntry() {}

    public LeaderboardEntry(int rank, String userId, String displayName, int score, String updatedAt) {
        this.rank = rank;
        this.userId = userId;
        this.displayName = displayName;
        this.score = score;
        this.updatedAt = updatedAt;
    }
}
