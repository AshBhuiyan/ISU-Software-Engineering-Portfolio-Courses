package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Login screen.
 * - Validates non-empty fields.
 * - Compares against credentials saved by SignupActivity (SharedPreferences).
 * - If "Remember me" is checked, we set logged_in=true so the app opens welcomed next time.
 * - "Show password" toggles the password visibility.
 * - On success, we go to MainActivity with USERNAME as an extra and finish() to remove Login from back stack.
 */
public class LoginActivity extends AppCompatActivity {

    // SharedPreferences keys same as other screens
    private static final String AUTH_PREFS   = "auth_prefs";
    private static final String KEY_USER     = "user";
    private static final String KEY_PASS     = "pass";
    private static final String KEY_LOGGED_IN= "logged_in";
    // Views
    private EditText usernameEditText;  // define username edittext variable
    private EditText passwordEditText;  // define password edittext variable
    private CheckBox showPwCheck;       // define checkbox to show/hide password
    private CheckBox rememberCheck;     // define checkbox to remember the user
    private Button loginButton;         // define login button variable
    private Button signupButton;        // define signup button variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // link to Login activity XML

        // Connecting views
        usernameEditText = findViewById(R.id.login_username_edt);
        passwordEditText = findViewById(R.id.login_password_edt);
        showPwCheck      = findViewById(R.id.login_showpw_chk);
        rememberCheck    = findViewById(R.id.login_remember_chk);
        loginButton      = findViewById(R.id.login_login_btn);    // link to login button in the Login activity XML
        signupButton     = findViewById(R.id.login_signup_btn);  // link to signup button in the Login activity XML

        // If the user came from Signup, prefill the username. This is for convenience.
        String prefill = getIntent().getStringExtra("PREFILL_USER");
        if (prefill != null) usernameEditText.setText(prefill);

        // Toggle password visibility. On/Off feature to show or hide password.
        showPwCheck.setOnCheckedChangeListener((btn, checked) -> {
            int type = checked
                    ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setInputType(type);
            // keeping the cursor at end after toggling
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // Login: validate + compare against saved credentials
        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();

            if (username.isEmpty()) { usernameEditText.setError("Username required"); return; }
            if (password.isEmpty()) { passwordEditText.setError("Password required"); return; }

            SharedPreferences sp = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
            String savedUser = sp.getString(KEY_USER, "");
            String savedPass = sp.getString(KEY_PASS, "");

            if (username.equals(savedUser) && password.equals(savedPass)) {
                // Feature to remember me keeps the user logged in next launch
                sp.edit().putBoolean(KEY_LOGGED_IN, rememberCheck.isChecked()).apply();

                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                // Go to Main with the username and finish so back won't return here
                startActivity(new Intent(LoginActivity.this, MainActivity.class)
                        .putExtra("USERNAME", username));
                finish();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Going back to Signup screen
        signupButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }
}
