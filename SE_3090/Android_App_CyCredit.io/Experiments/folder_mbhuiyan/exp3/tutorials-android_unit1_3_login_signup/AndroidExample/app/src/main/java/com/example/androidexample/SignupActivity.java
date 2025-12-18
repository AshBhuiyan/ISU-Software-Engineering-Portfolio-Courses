package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Signup screen.
 * - Validates: non-empty username, password >= 6 characters, confirm matches.
 * - Saves username/password into SharedPreferences (very basic demo, not secure).
 * - Then navigates to Login and prefills the username.
 */
public class SignupActivity extends AppCompatActivity {

    // SharedPreferences keys (same family as Login/Main)
    private static final String AUTH_PREFS   = "auth_prefs";
    private static final String KEY_USER     = "user";
    private static final String KEY_PASS     = "pass";

    // Views
    private EditText usernameEditText; // define username edittext variable
    private EditText passwordEditText; // define password edittext variable
    private EditText confirmEditText;  // define confirm edittext variable
    private Button   loginButton;      // define login button variable
    private Button   signupButton;     // define signup button variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // links to activity_signup.xml

        // Connect views
        usernameEditText = findViewById(R.id.signup_username_edt);  // link to username edtext in the Signup activity XML
        passwordEditText = findViewById(R.id.signup_password_edt);  // link to password edtext in the Signup activity XML
        confirmEditText = findViewById(R.id.signup_confirm_edt);    // link to confirm edtext in the Signup activity XML
        loginButton = findViewById(R.id.signup_login_btn);          // link to login button in the Signup activity XML
        signupButton = findViewById(R.id.signup_signup_btn);        // link to signup button in the Signup activity XML

        // Already have an account? Go to Login
        loginButton.setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class)));

        // Create account
        signupButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            String confirm  = confirmEditText.getText().toString();

            // Basic validation with friendly errors
            if (username.isEmpty()) { usernameEditText.setError("Username required"); return; }
            if (password.length() < 6) { passwordEditText.setError("Min 6 characters"); return; }
            if (!password.equals(confirm)) { confirmEditText.setError("Passwords do not match"); return; }

            // Save to SharedPreferences (demo purpose only)
            SharedPreferences sp = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
            sp.edit()
                    .putString(KEY_USER, username)
                    .putString(KEY_PASS, password)
                    .apply();

            Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();

            // Send user to Login with username prefilled
            startActivity(new Intent(SignupActivity.this, LoginActivity.class)
                    .putExtra("PREFILL_USER", username));
            finish(); // don't come back here on back press
        });
    }
}
