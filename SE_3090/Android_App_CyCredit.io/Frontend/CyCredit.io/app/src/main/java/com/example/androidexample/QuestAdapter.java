package cycredit.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.ViewHolder> {

    public interface OnQuestClick { void onClick(String questId); }

    private List<QuestsActivity.QuestModel> quests;
    private Map<String, QuestsActivity.UserQuestModel> progressMap;
    private final OnQuestClick onQuestClick;

    public QuestAdapter(List<QuestsActivity.QuestModel> quests,
                        List<QuestsActivity.UserQuestModel> progress,
                        OnQuestClick listener) {
        if (quests == null) quests = new ArrayList<QuestsActivity.QuestModel>();
        this.quests = quests;
        this.onQuestClick = listener;

        this.progressMap = new HashMap<String, QuestsActivity.UserQuestModel>();
        if (progress != null) {
            for (QuestsActivity.UserQuestModel p : progress) {
                if (p != null && p.questId != null) this.progressMap.put(p.questId, p);
            }
        }
    }

    public void updateData(List<QuestsActivity.QuestModel> quests,
                           List<QuestsActivity.UserQuestModel> progress) {
        this.quests = (quests != null) ? quests : new ArrayList<QuestsActivity.QuestModel>();
        this.progressMap.clear();
        if (progress != null) {
            for (QuestsActivity.UserQuestModel p : progress) {
                if (p != null && p.questId != null) this.progressMap.put(p.questId, p);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        final QuestsActivity.QuestModel q = quests.get(position);
        h.title.setText(q.title);
        h.subtitle.setText(q.description);

        QuestsActivity.UserQuestModel p = progressMap.get(q.questId);
        int percent = (p != null) ? p.progressPercent : 0;

        String statusLabel = "IN_PROGRESS";
        if (p != null && p.status != null && !p.status.isEmpty()) {
            statusLabel = p.status;
        }

        // Show status + percentage text (e.g., "IN_PROGRESS 20%")
        h.status.setText(statusLabel + " " + percent + "%");

        // Hide the progress bar so it doesn't show at all
        h.progress.setVisibility(View.GONE);

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (onQuestClick != null && q.questId != null) onQuestClick.onClick(q.questId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, status;
        ProgressBar progress;
        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            subtitle = itemView.findViewById(R.id.txt_subtitle);
            status = itemView.findViewById(R.id.txt_status);
            progress = itemView.findViewById(R.id.progress);
        }
    }
}
