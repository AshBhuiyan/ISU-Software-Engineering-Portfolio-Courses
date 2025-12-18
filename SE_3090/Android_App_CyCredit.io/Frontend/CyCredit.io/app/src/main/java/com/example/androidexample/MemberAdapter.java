package cycredit.io;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cycredit.io.guilds.GuildMember;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {

    private final List<GuildMember> data = new ArrayList<>();

    public void setItems(List<GuildMember> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        GuildMember m = data.get(pos);
        String name = (m.username == null) ? "" : m.username.trim();
        String title = TextUtils.isEmpty(name) ? ("ID " + m.id) : (name + " (ID " + m.id + ")");
        h.title.setText(title);
        h.role.setText(m.role == null ? "MEMBER" : m.role);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, role;
        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvMemberTitle);
            role  = v.findViewById(R.id.tvMemberRole);
        }
    }
}
