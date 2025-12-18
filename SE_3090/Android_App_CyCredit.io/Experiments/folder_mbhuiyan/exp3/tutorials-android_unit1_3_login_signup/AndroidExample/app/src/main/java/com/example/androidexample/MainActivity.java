package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Home screen.
 * - If user is logged in (or we just came from Login with a username), we show "Welcome <user>" and a Logout button.
 * - Otherwise we show "Home Page" with Login / Signup buttons.
 * - State is stored in SharedPreferences (tiny on-device key/value storage).
 */
public class MainActivity extends AppCompatActivity {

    // SharedPreferences keys used by Login/Signup/Main //
    private static final String AUTH_PREFS   = "auth_prefs";
    private static final String KEY_USER     = "user";
    private static final String KEY_PASS     = "pass";
    private static final String KEY_LOGGED_IN= "logged_in";

    // Views

    private TextView messageText;   // define message textview variable
    private TextView usernameText;  // define username textview variable
    private Button loginButton;     // define login button variable
    private Button signupButton;    // define signup button variable
    private Button   logoutButton;  // define logout button variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // link to Main activity XML

        // Connecting up UI references //
        messageText = findViewById(R.id.main_msg_txt);      // link to message textview in the Main activity XML
        usernameText = findViewById(R.id.main_username_txt);// link to username textview in the Main activity XML
        loginButton = findViewById(R.id.main_login_btn);    // link to login button in the Main activity XML
        signupButton = findViewById(R.id.main_signup_btn);  // link to signup button in the Main activity XML
        logoutButton = findViewById(R.id.main_logout_btn);  // link to logout button in the Main activity XML

        // Buttons: go to Login / Signup / Logout //
        loginButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        signupButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SignupActivity.class)));

        logoutButton.setOnClickListener(v -> {
            // Clear "logged in" flag; keep saved username/password for future logins
            SharedPreferences sp = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
            sp.edit().putBoolean(KEY_LOGGED_IN, false).apply();

            // Show logged-out UI and go to Login so user understands they’re logged out
            applyLoggedOutUI();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // prevent back stack returning here in logged-in state
        });

        // Deciding which UI to show when we arrive on this screen
        decideAndApplyUI();
    }

    /** Decides if we should show "Welcome" or "Home Page". */
    private void decideAndApplyUI() {
        SharedPreferences sp = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
        boolean loggedIn = sp.getBoolean(KEY_LOGGED_IN, false);

        // If LoginActivity just sent us a username, use it (fresh login case)
        String incomingUser = getIntent().getStringExtra("USERNAME");

        if (loggedIn || incomingUser != null) {
            // Prefer incoming user; else use saved one
            String name = (incomingUser != null) ? incomingUser : sp.getString(KEY_USER, "User");
            applyLoggedInUI(name);
        } else {
            applyLoggedOutUI();
        }
    }

    /** Show "Welcome (user name)" + Logout; hide Login/Signup. */
    private void applyLoggedInUI(String name) {
        messageText.setText("Welcome");
        usernameText.setText(name);
        usernameText.setVisibility(View.VISIBLE);

        loginButton.setVisibility(View.GONE);
        signupButton.setVisibility(View.GONE);
        logoutButton.setVisibility(View.VISIBLE);
    }

    /** Show "Home Page" + Login/Signup; hide username + Logout. */
    private void applyLoggedOutUI() {
        messageText.setText("Home Page");
        usernameText.setVisibility(View.INVISIBLE);

        loginButton.setVisibility(View.VISIBLE);
        signupButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.GONE);
    }
}
