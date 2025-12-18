package cycredit.io.guilds;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cycredit.io.R;

public class GuildAdapter extends RecyclerView.Adapter<GuildAdapter.VH> {

    public interface OnItemClick {
        void onClick(int guildId, String guildName);
    }

    private final List<Guild> data = new ArrayList<>();
    private OnItemClick onItemClick;

    public GuildAdapter(OnItemClick click) {
        this.onItemClick = click;
    }

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setItems(List<Guild> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    private Guild getItemAt(int position) {
        if (position < 0 || position >= data.size()) return null;
        return data.get(position);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guild, parent, false);
        VH vh = new VH(v);
        View root = vh.itemView;
        root.setClickable(true);
        root.setFocusable(true);
        root.setOnClickListener(view -> {
            int pos = vh.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Guild g = getItemAt(pos);
            if (g == null) return;
            int gid = (g.id != 0) ? g.id : 0;
            String gname = (g.name != null) ? g.name : "";
            if (onItemClick != null) onItemClick.onClick(gid, gname);
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Guild g = data.get(position);
        if (g == null) return;
        h.name.setText(g.name != null ? g.name : "");
        h.desc.setText(g.description == null ? "" : g.description);
        h.count.setText(String.valueOf(g.membersCount));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, desc, count;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvGuildName);
            desc = itemView.findViewById(R.id.tvGuildDesc);
            count = itemView.findViewById(R.id.tvMemberCount);
        }
    }
}
