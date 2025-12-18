package cycredit.io.guilds;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;

import org.json.JSONObject;

import cycredit.io.GuildService;
import cycredit.io.R;
import cycredit.io.Session;

public class CreateGuildActivity extends AppCompatActivity {

    private EditText etName;
    private ProgressBar progress;
    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_guild);

        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        etName = findViewById(R.id.etGuildName);
        progress = findViewById(R.id.progress);
        Button btnCreate = findViewById(R.id.btnCreateGuild);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) userId = Session.getUserId(this);
        Toast.makeText(this, "CreateGuild USER_ID=" + userId, Toast.LENGTH_SHORT).show();

        btnCreate.setOnClickListener(v -> submit());
    }

    private void submit() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etName.setError("Required");
            return;
        }
        if (userId <= 0) {
            Toast.makeText(this, "USER_ID required", Toast.LENGTH_SHORT).show();
            return;
        }
        progress.setVisibility(android.view.View.VISIBLE);
        GuildService.createGuild(this, name, userId, new GuildService.JsonCallback() {
            @Override public void onSuccess(JSONObject json) {
                progress.setVisibility(android.view.View.GONE);
                Toast.makeText(CreateGuildActivity.this, "Guild created", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onError(VolleyError e) {
                progress.setVisibility(android.view.View.GONE);
            }
        });
    }
}
