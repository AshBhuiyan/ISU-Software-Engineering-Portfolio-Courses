package cycredit.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cycredit.io.R;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> items = new ArrayList<>();

    public void setItems(List<LeaderboardEntry> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        LeaderboardEntry e = items.get(position);
        h.txtRank.setText("#" + e.rank);
        h.txtName.setText(e.displayName != null ? e.displayName : ("User " + e.userId));
        h.txtScore.setText(String.valueOf(e.score));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank, txtName, txtScore;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRank  = itemView.findViewById(R.id.txt_rank);
            txtName  = itemView.findViewById(R.id.txt_name);
            txtScore = itemView.findViewById(R.id.txt_score);
        }
    }
}
