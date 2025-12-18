package cycredit.io;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CharacterSelectionActivity extends AppCompatActivity implements CharacterAdapter.OnCharacterClickListener {

    private RecyclerView recyclerView;
    private CharacterAdapter adapter;
    private List<CharacterModel> characterList;

    private Button nextBtn;
    private ImageButton backBtn;
    private int userId;
    private int characterId = -1;
    private String email;

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_selection);

        recyclerView = findViewById(R.id.characterRecycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Navigation buttons
        nextBtn = findViewById(R.id.btn_next);
        backBtn = findViewById(R.id.btn_back);

        userId = getIntent().getIntExtra("USER_ID", -1);
        // Fallback to prefs if intent extra is missing
        if (userId <= 0) {
            userId = cycredit.io.util.UserPrefs.userId(this);
        }
        email = getIntent().getStringExtra("EMAIL");
        
        // Defensive check for required extras
        if (userId <= 0 || email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        queue = Volley.newRequestQueue(this);

        // Character list setup - ordered as specified
        characterList = new ArrayList<>();
        characterList.add(new CharacterModel("Cy the Cardinal", R.drawable.ic_cy, "Iowa State's fearless mascot — always ready for a challenge!"));
        characterList.add(new CharacterModel("Mekhi", R.drawable.ic_mekhi, "Ambitious software engineer"));
        characterList.add(new CharacterModel("Ash", R.drawable.ic_ash, "Creative problem-solver"));
        characterList.add(new CharacterModel("Carson", R.drawable.ic_carson, "Backend wizard"));
        characterList.add(new CharacterModel("Chase", R.drawable.ic_chase, "Database master"));
        characterList.add(new CharacterModel("Dr. Mitra", R.drawable.ic_mitra, "Professor and mentor"));
        characterList.add(new CharacterModel("Swarna", R.drawable.ic_swarna, "Strategic and detail-oriented"));
        characterList.add(new CharacterModel("Wendy Wintersteen", R.drawable.ic_wendy, "President of ISU"));

        adapter = new CharacterAdapter(characterList, this);
        recyclerView.setAdapter(adapter);

        // Navigation logic
        nextBtn.setEnabled(false); // Disable until character is picked

        nextBtn.setOnClickListener(v -> {
            if (characterId == -1) {
                Toast.makeText(this, "Please select a character first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CharacterSelectionActivity.this, CounterActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("EMAIL", email);
            startActivity(intent);
        });

        backBtn.setOnClickListener(v -> {
            if (isTaskRoot()) {
                Intent i = new Intent(this, cycredit.io.LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            } else {
                finish();
            }
        });
    }

    @Override
    public void onCharacterClick(CharacterModel character) {
        Toast.makeText(this, "Selected: " + character.getName(), Toast.LENGTH_SHORT).show();
        // Save character to preferences immediately
        cycredit.io.util.UserPrefs.saveCharacter(this, character.getName(), -1); // ID will be updated after fetch
        postCharacter(userId, character.getName());
        nextBtn.setEnabled(true); // Enable Next once user selects
    }

    // ------------------ POST ------------------
    private void postCharacter(int userId, String characterName) {
        String url = BASE_URL + "/avatar/" + userId + "/" + characterName;
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(this, "✅ Character saved: " + characterName, Toast.LENGTH_SHORT).show();
                    fetchCharacterAfterSave(userId);
                },
                error -> Toast.makeText(this, "❌ POST failed: " + error.toString(), Toast.LENGTH_LONG).show()
        );
        queue.add(postRequest);
    }

    // ------------------ Auto-fetch character after save ------------------
    private void fetchCharacterAfterSave(int userId) {
        String url = BASE_URL + "/avatar/" + userId;
        StringRequest getRequest = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        characterId = json.getInt("id");
                        String name = json.getString("avatarName");
                        // Update character in preferences with the fetched ID
                        cycredit.io.util.UserPrefs.saveCharacter(CharacterSelectionActivity.this, name, characterId);
                        nextBtn.setEnabled(true);
                    } catch (Exception e) {
                        // Silent fail
                    }
                },
                error -> {
                    // Silent fail
                }
        );
        queue.add(getRequest);
    }
}
