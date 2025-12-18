package cycredit.io;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import cycredit.io.guilds.ApiClient;
import cycredit.io.util.DisplayFormatUtils;

public class RestActivity extends AppCompatActivity {

    private int userId;

    private TextView turnsText;
    private TextView statusText;
    private Button restButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("State Gym – Rest");
        }

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) {
            userId = Session.getUserId(this);
        }

        turnsText = findViewById(R.id.turns_text);
        statusText = findViewById(R.id.rest_status);
        restButton = findViewById(R.id.rest_button);
        progressBar = findViewById(R.id.progress_bar);

        restButton.setOnClickListener(v -> performRest());
        findViewById(R.id.open_wellness_button).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, WellnessActivity.class)
                    .putExtra("USER_ID", userId)
                    .putExtra("EMAIL", getIntent().getStringExtra("EMAIL")));
        });

        loadResource();
    }

    private void loadResource() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        String url = ApiClient.BASE_URL + "/resource/" + userId;
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    try {
                        JSONObject json = new JSONObject(response);
                        int turns = json.optInt("turnsLeft", 0);
        turnsText.setText(DisplayFormatUtils.formatTurns(turns));
                        restButton.setEnabled(turns > 0);
                        statusText.setText("");
                    } catch (JSONException e) {
                        Toast.makeText(this, "Failed to parse resource", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Failed to load resource", Toast.LENGTH_SHORT).show();
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void performRest() {
        restButton.setEnabled(false);
        progressBar.setVisibility(android.view.View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiClient.BASE_URL + "/rest/skip?userId=" + userId,
                null,
                response -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    int turns = response.optInt("turnsRemaining", 0);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        statusText.setText("You took a break. " + DisplayFormatUtils.formatTurns(turns));
                        HudSyncHelper.refreshHud(this, userId);
                    } else {
                        String error = response.optString("error", "Unable to rest.");
                        statusText.setText(error);
                    }
                    restButton.setEnabled(turns > 0);
                    turnsText.setText(DisplayFormatUtils.formatTurns(turns));
                },
                error -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    restButton.setEnabled(true);
                    Toast.makeText(this, "Rest failed", Toast.LENGTH_SHORT).show();
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

