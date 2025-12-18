package cycredit.io;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import cycredit.io.R;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private List<CharacterModel> characterList;
    private OnCharacterClickListener listener;

    public interface OnCharacterClickListener {
        void onCharacterClick(CharacterModel character);
    }

    public CharacterAdapter(List<CharacterModel> characterList, OnCharacterClickListener listener) {
        this.characterList = characterList;
        this.listener = listener;
    }

    @Override
    public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.character_item, parent, false);
        return new CharacterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CharacterViewHolder holder, int position) {
        CharacterModel character = characterList.get(position);
        holder.imageView.setImageResource(character.getImageResId());
        holder.textView.setText(character.getName());
        holder.itemView.setOnClickListener(v -> listener.onCharacterClick(character));
    }

    @Override
    public int getItemCount() {
        return characterList.size();
    }

    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public CharacterViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.characterImage);
            textView = itemView.findViewById(R.id.characterName);
        }
    }
}
