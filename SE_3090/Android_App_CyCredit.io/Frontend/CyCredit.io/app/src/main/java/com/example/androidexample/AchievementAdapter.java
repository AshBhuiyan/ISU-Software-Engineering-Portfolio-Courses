package cycredit.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private List<AchievementsActivity.AchievementModel> data;

    public AchievementAdapter(List<AchievementsActivity.AchievementModel> data) {
        this.data = (data != null) ? data : new ArrayList<AchievementsActivity.AchievementModel>();
    }

    public void updateData(List<AchievementsActivity.AchievementModel> fresh) {
        this.data = (fresh != null) ? fresh : new ArrayList<AchievementsActivity.AchievementModel>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        AchievementsActivity.AchievementModel a = data.get(pos); // Java 8 explicit type
        holder.title.setText(a.title);
        holder.subtitle.setText(a.subtitle);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            subtitle = itemView.findViewById(R.id.txt_subtitle);
        }
    }
}
