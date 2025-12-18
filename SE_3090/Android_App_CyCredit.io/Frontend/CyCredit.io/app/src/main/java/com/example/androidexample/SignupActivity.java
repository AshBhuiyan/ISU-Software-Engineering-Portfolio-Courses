package cycredit.io;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import cycredit.io.PasswordStrengthValidator;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmEditText;
    private Button signupButton;
    private Button loginButton;
    private Button backButton;
    private RequestQueue requestQueue;

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        EditText passwordField = findViewById(R.id.signup_password_edt);
        TextView feedbackView = findViewById(R.id.password_feedback_txt);

        PasswordStrengthValidator.attachValidator(passwordField, feedbackView);

        usernameEditText = findViewById(R.id.signup_username_edt);
        passwordEditText = findViewById(R.id.signup_password_edt);
        confirmEditText = findViewById(R.id.signup_confirm_edt);
        signupButton = findViewById(R.id.signup_signup_btn);
        loginButton = findViewById(R.id.signup_login_btn);
        backButton = findViewById(R.id.back_btn);
        requestQueue = Volley.newRequestQueue(this);

        signupButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirm = confirmEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords don’t match", Toast.LENGTH_SHORT).show();
                return;
            }
            performSignupVolley(username, password);
        });

        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        backButton.setOnClickListener(v -> {
            if (isTaskRoot()) {
                Intent i = new Intent(this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            } else {
                finish();
            }
        });
    }

    private static final String EMAIL_DOMAIN = "@iastate.edu";
    
    private String normalizeUsername(String raw) {
        if (raw == null) return "";
        String u = raw.trim();
        if (u.isEmpty()) return "";
        if (u.contains("@")) return u;
        if (u.endsWith(EMAIL_DOMAIN)) return u;
        return u + EMAIL_DOMAIN;
    }
    
    private String toEmailId(String userTyped) {
        return normalizeUsername(userTyped);
    }

    private void performSignupVolley(String username, String password) {
        String url = BASE_URL + "/users";
        String emailId = toEmailId(username);

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Parse and persist backend user ID if response contains user data
                    try {
                        if (response != null && !response.trim().isEmpty()) {
                            org.json.JSONObject obj = new org.json.JSONObject(response);
                            int backendUserId = -1;
                            if (obj.has("id")) {
                                backendUserId = obj.getInt("id");
                            }
                            // Fallback: if nested
                            if (backendUserId == -1 && obj.has("user")) {
                                org.json.JSONObject u = obj.getJSONObject("user");
                                if (u.has("id")) {
                                    backendUserId = u.getInt("id");
                                }
                            }
                            if (backendUserId != -1) {
                                cycredit.io.util.UserPrefs.saveUserId(this, backendUserId);
                            }
                        }
                    } catch (Exception e) {
                        // Silent fail - user will get ID on login
                    }
                    
                    showDialog("Signup Successful", 
                            (response != null && !response.trim().isEmpty()) ? response : "Account created successfully!", 
                            () -> {
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            });
                },
                error -> {
                    String errorMsg = cycredit.io.util.ErrorHandler.getErrorMessage(error);
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Signup failed. Please try again.";
                    }
                    showDialog("Signup Failed", errorMsg, null);
                }) {

            @Override
            public byte[] getBody() {
                String body = String.format(
                        "{\"name\":\"%s\",\"emailId\":\"%s\",\"password\":\"%s\"}",
                        username, emailId, password
                );
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=UTF-8";
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        requestQueue.add(req);
    }
    
    private void showDialog(String title, String message, Runnable onOk) {
        // Ensure message is never null or empty
        if (message == null || message.trim().isEmpty()) {
            message = "An error occurred. Please try again.";
        }
        
        // Ensure title is never null or empty
        if (title == null || title.trim().isEmpty()) {
            title = "Error";
        }
        
        // Create dialog with explicit styling to ensure visibility
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    if (onOk != null) onOk.run();
                })
                .create();
        
        dialog.show();
        
        // Ensure message text is visible (fix for Material3 theme issues)
        android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
            messageView.setTextSize(14f);
        }
        
        // Ensure title text is visible
        android.widget.TextView titleView = dialog.findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
        }
    }
}
