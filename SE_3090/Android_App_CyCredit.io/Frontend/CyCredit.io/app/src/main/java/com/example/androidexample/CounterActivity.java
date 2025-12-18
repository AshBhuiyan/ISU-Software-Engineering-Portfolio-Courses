package cycredit.io;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.content.Intent;
import android.widget.ImageButton; // <-- added

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class CounterActivity extends AppCompatActivity {

    private EditText startingFundsInput;
    private EditText startingCreditInput;
    private Button saveBtn;
    private ImageButton backBtn;   // <-- changed to ImageButton

    // CRUD buttons
    private Button postBtn;
    private Button getBtn;
    private Button putBtn;
    private Button deleteBtn;
    private Button toggleControlsBtn;
    private View crudButtonsContainer;

    private TextView responseText;
    private TextView creditHintText;
    
    private boolean isControlsExpanded = false;

    private int userId;
    private int resourceId = -1; // dynamic resource id
    private String email;

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;
    private RequestQueue queue;

    private int minCreditAllowed = 300;
    private int maxCreditAllowed = 850;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter);

        startingFundsInput = findViewById(R.id.starting_funds_input);
        startingCreditInput = findViewById(R.id.starting_credit_input);
        saveBtn = findViewById(R.id.start_btn);
        backBtn = findViewById(R.id.back_btn); // <-- now matches ImageButton in XML
        responseText = findViewById(R.id.response_text);
        creditHintText = findViewById(R.id.credit_hint_text);

        // CRUD buttons
        postBtn = findViewById(R.id.btn_post);
        getBtn = findViewById(R.id.btn_get);
        putBtn = findViewById(R.id.btn_put);
        deleteBtn = findViewById(R.id.btn_delete);
        toggleControlsBtn = findViewById(R.id.btn_toggle_controls);
        crudButtonsContainer = findViewById(R.id.crud_buttons_container);

        queue = Volley.newRequestQueue(this);

        // Retrieve user info with defensive checks
        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) userId = cycredit.io.util.UserPrefs.userId(this);
        email = getIntent().getStringExtra("EMAIL");
        
        if (userId <= 0) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Go to Campus Map button wiring
        Button goMapBtn = findViewById(R.id.btn_go_map);
        goMapBtn.setOnClickListener(v -> {
            if (userId <= 0) {
                Toast.makeText(this, "Missing user id. Try saving resource first.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(CounterActivity.this, cycredit.io.MapActivity.class);
            i.putExtra("USER_ID", userId);
            i.putExtra("EMAIL", email);
            startActivity(i);
        });

        // Update credit range dynamically
        startingFundsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCreditRangeHint();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // ---------------- SAVE BUTTON ----------------
        saveBtn.setOnClickListener(v -> {
            String fundsStr = startingFundsInput.getText().toString().trim();
            String creditStr = startingCreditInput.getText().toString().trim();

            if (fundsStr.isEmpty() || creditStr.isEmpty()) {
                Toast.makeText(this, "Please fill both fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            double funds;
            double credit;
            try {
                funds = Double.parseDouble(fundsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid funds amount. Please enter a valid number.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                credit = Double.parseDouble(creditStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid credit score. Please enter a valid number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (funds < 0 || funds > 10000) {
                Toast.makeText(this, "Funds must be between 0 and 10,000", Toast.LENGTH_SHORT).show();
                return;
            }

            updateCreditRangeHint();
            if (credit < minCreditAllowed || credit > maxCreditAllowed) {
                Toast.makeText(this,
                        "Invalid credit for your starting money.\nAllowed range: " + minCreditAllowed + "–" + maxCreditAllowed,
                        Toast.LENGTH_LONG).show();
                return;
            }

            postPlayerResource(userId, funds, credit);
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

        // ---------------- DROPDOWN TOGGLE ----------------
        toggleControlsBtn.setOnClickListener(v -> {
            isControlsExpanded = !isControlsExpanded;
            if (isControlsExpanded) {
                crudButtonsContainer.setVisibility(View.VISIBLE);
                toggleControlsBtn.setText("Resource Controls ▲");
            } else {
                crudButtonsContainer.setVisibility(View.GONE);
                toggleControlsBtn.setText("Resource Controls ▼");
            }
        });

        // ---------------- CRUD BUTTONS ----------------
        // POST
        postBtn.setOnClickListener(v -> {
            String fundsStr = startingFundsInput.getText().toString().trim();
            String creditStr = startingCreditInput.getText().toString().trim();

            if (fundsStr.isEmpty() || creditStr.isEmpty()) {
                Toast.makeText(this, "Please fill both fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            double funds;
            double credit;
            try {
                funds = Double.parseDouble(fundsStr);
                credit = Double.parseDouble(creditStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input. Please enter valid numbers.", Toast.LENGTH_SHORT).show();
                return;
            }

            postPlayerResource(userId, funds, credit);
        });

        // GET
        getBtn.setOnClickListener(v -> getPlayerResource(userId));

        // PUT
        putBtn.setOnClickListener(v -> {
            if (resourceId == -1) {
                Toast.makeText(this, "Please GET resource first.", Toast.LENGTH_SHORT).show();
                return;
            }

            String fundsStr = startingFundsInput.getText().toString().trim();
            String creditStr = startingCreditInput.getText().toString().trim();

            if (fundsStr.isEmpty() || creditStr.isEmpty()) {
                Toast.makeText(this, "Please fill both fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            double funds;
            double credit;
            try {
                funds = Double.parseDouble(fundsStr);
                credit = Double.parseDouble(creditStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input. Please enter valid numbers.", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePlayerResource(resourceId, funds + 100, credit, 5);
        });

        // DELETE
        deleteBtn.setOnClickListener(v -> {
            if (resourceId == -1) {
                Toast.makeText(this, "Please GET resource first.", Toast.LENGTH_SHORT).show();
                return;
            }

            deletePlayerResource(resourceId);
        });
    }

    // -------------------- Credit Tier Logic --------------------
    private void updateCreditRangeHint() {
        String fundsStr = startingFundsInput.getText().toString().trim();
        if (fundsStr.isEmpty()) {
            creditHintText.setText("Enter a starting amount to see valid credit range.");
            return;
        }

        double funds;
        try {
            funds = Double.parseDouble(fundsStr);
        } catch (NumberFormatException e) {
            creditHintText.setText("Enter a valid number for starting amount.");
            return;
        }

        if (funds <= 500) {
            minCreditAllowed = 300;
            maxCreditAllowed = 650;
        } else if (funds <= 2000) {
            minCreditAllowed = 500;
            maxCreditAllowed = 700;
        } else if (funds <= 5000) {
            minCreditAllowed = 600;
            maxCreditAllowed = 750;
        } else if (funds <= 8000) {
            minCreditAllowed = 650;
            maxCreditAllowed = 800;
        } else {
            minCreditAllowed = 725;
            maxCreditAllowed = 850;
        }

        creditHintText.setText("Allowed Credit Score Range: " + minCreditAllowed + " – " + maxCreditAllowed);
    }

    /**
     * Trim number to avoid trailing .0 in URLs
     */
    private static String trimNumber(double n) {
        if (n == Math.rint(n)) return String.valueOf((long) n);
        return new java.text.DecimalFormat("0.##").format(n).replace(",", "");
    }

    // -------------------- POST --------------------
    private void postPlayerResource(int userId, double funds, double credit) {
        String url = BASE_URL + "/resource/" + userId + "/" + trimNumber(funds) + "/" + trimNumber(credit);

        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(this, "✅ Saved to backend: " + response, Toast.LENGTH_LONG).show();
                    responseText.setText("✅ Resource saved!\nFunds: $" + funds + "\nCredit: " + credit);

                    // Auto-fetch resource after saving
                    fetchResourceAfterSave(userId);
                },
                error -> {
                    String details = (error.networkResponse != null)
                            ? "HTTP " + error.networkResponse.statusCode
                            : error.toString();
                    
                    // Friendly message for HTTP 5xx errors
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        if (statusCode >= 500 && statusCode <= 599) {
                            Toast.makeText(this, "Server error (HTTP " + statusCode + "). Please try again.", Toast.LENGTH_LONG).show();
                            android.util.Log.e("CounterActivity", "Save failed: " + details, error);
                        } else {
                            Toast.makeText(this, "❌ Save failed: " + details, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "❌ Save failed: " + details, Toast.LENGTH_LONG).show();
                    }
                    responseText.setText("❌ Error: " + details);
                    // Never finish() on error - let user retry
                }
        );

        queue.add(postRequest);
    }

    // -------------------- Auto-fetch after save --------------------
    private void fetchResourceAfterSave(int userId) {
        String url = BASE_URL + "/resource/" + userId;

        StringRequest getRequest = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        resourceId = json.getInt("id");
                        double money = json.getDouble("money");
                        double credit = json.getDouble("credit");

                        responseText.setText("✅ Resource saved!\nID: " + resourceId +
                                "\nMoney: $" + money + "\nCredit: " + credit);
                    } catch (Exception e) {
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    // Silent fail - resource might not exist yet
                }
        );

        queue.add(getRequest);
    }

    // -------------------- GET --------------------
    private void getPlayerResource(int userId) {
        String url = BASE_URL + "/resource/" + userId;

        StringRequest getRequest = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        resourceId = json.getInt("id");
                        double money = json.getDouble("money");
                        double credit = json.getDouble("credit");

                        responseText.setText("✅ Resource fetched!\nID: " + resourceId +
                                "\nMoney: $" + money + "\nCredit: " + credit);
                        Toast.makeText(this, "Resource loaded successfully!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "❌ GET failed: " + error.toString(), Toast.LENGTH_LONG).show()
        );

        queue.add(getRequest);
    }

    // -------------------- PUT --------------------
    private void updatePlayerResource(int resourceId, double money, double credit, int turnsLeft) {
        String url = BASE_URL + "/resource/" + resourceId;
        String jsonBody = String.format("{\"money\":%.2f,\"credit\":%.2f,\"turnsLeft\":%d}", money, credit, turnsLeft);

        StringRequest putRequest = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    Toast.makeText(this, "✅ Resource updated!", Toast.LENGTH_SHORT).show();
                    responseText.setText("PUT Response:\n" + response);
                },
                error -> Toast.makeText(this, "❌ PUT failed: " + error.toString(), Toast.LENGTH_LONG).show()
        ) {
            @Override
            public byte[] getBody() {
                return jsonBody.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        queue.add(putRequest);
    }

    // -------------------- DELETE --------------------
    private void deletePlayerResource(int resourceId) {
        String url = BASE_URL + "/resource/" + resourceId;

        StringRequest deleteRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "✅ Resource deleted!", Toast.LENGTH_SHORT).show();
                    responseText.setText(response);
                    this.resourceId = -1; // reset after deletion
                },
                error -> Toast.makeText(this, "❌ DELETE failed: " + error.toString(), Toast.LENGTH_LONG).show()
        );

        queue.add(deleteRequest);
    }
}
