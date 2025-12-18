package cycredit.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import cycredit.io.GuildInvite;

import cycredit.io.R;

public class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.VH> {

    public interface OnInviteAction {
        void onAccept(GuildInvite invite, int position);
        void onDecline(GuildInvite invite, int position);
    }

    private final List<cycredit.io.GuildInvite> data = new ArrayList<>();
    private final OnInviteAction callbacks;

    public InviteAdapter(OnInviteAction cb) { this.callbacks = cb; }

    public void setItems(List<GuildInvite> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invite, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        GuildInvite inv = data.get(pos);
        h.title.setText("Invite #" + inv.id + " → guild " + inv.guildId);
        h.status.setText(inv.status == null ? "pending" : inv.status);

        boolean pending = "pending".equalsIgnoreCase(inv.status);
        h.btnAccept.setEnabled(pending);
        h.btnDecline.setEnabled(pending);
        h.spinner.setVisibility(View.GONE);

        h.btnAccept.setOnClickListener(v -> {
            h.btnAccept.setEnabled(false);
            h.btnDecline.setEnabled(false);
            h.spinner.setVisibility(View.VISIBLE);
            callbacks.onAccept(inv, h.getBindingAdapterPosition());
        });

        h.btnDecline.setOnClickListener(v -> {
            h.btnAccept.setEnabled(false);
            h.btnDecline.setEnabled(false);
            h.spinner.setVisibility(View.VISIBLE);
            callbacks.onDecline(inv, h.getBindingAdapterPosition());
        });
    }

    @Override public int getItemCount() { return data.size(); }

    public void removeRow(int position) {
        if (position < 0 || position >= data.size()) return;
        data.remove(position);
        notifyItemRemoved(position);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, status;
        Button btnAccept, btnDecline;
        ProgressBar spinner;
        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvInviteTitle);
            status = v.findViewById(R.id.tvInviteStatus);
            btnAccept = v.findViewById(R.id.btnInviteAccept);
            btnDecline = v.findViewById(R.id.btnInviteDecline);
            spinner = v.findViewById(R.id.inviteRowSpinner);
        }
    }
}
