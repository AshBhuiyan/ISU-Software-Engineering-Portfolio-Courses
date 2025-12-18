package cycredit.io.guilds;

import org.json.JSONObject;

public class Guild {
    public int id;
    public String name;
    public String description;
    public String code;
    public int membersCount;

    public static Guild fromJson(JSONObject o) {
        Guild g = new Guild();
        if (o == null) return g;
        g.id = o.optInt("id", o.optInt("guildId", 0));
        g.name = o.optString("name", o.optString("guildName", "Unnamed"));
        g.description = o.optString("description", "");
        g.code = o.optString("code", o.optString("inviteCode", ""));
        g.membersCount = o.optInt("membersCount", o.optInt("memberCount", o.optInt("size", 0)));
        return g;
    }
}
