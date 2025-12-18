package cycredit.io;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.checkbox.MaterialCheckBox;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signupButton;
    private Button deleteUserButton;
    private Button changePassButton;

    private RequestQueue requestQueue;
    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("LOGIN_VOLLEY", "========================================");
        Log.e("LOGIN_VOLLEY", "=== LoginActivity.onCreate() CALLED ===");
        Log.e("LOGIN_VOLLEY", "========================================");
        
        setContentView(R.layout.activity_login);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        usernameEditText = findViewById(R.id.login_username_edt);
        passwordEditText = findViewById(R.id.login_password_edt);
        MaterialCheckBox rememberCb = findViewById(R.id.cb_remember_me);
        loginButton = findViewById(R.id.login_login_btn);
        signupButton = findViewById(R.id.login_signup_btn);
        deleteUserButton = findViewById(R.id.delete_user_btn);
        changePassButton = findViewById(R.id.change_pass_btn);
        Button backButton = findViewById(R.id.back_btn);

        // Prefill on load if remember me was checked
        if (cycredit.io.util.UserPrefs.rememberMe(this)) {
            if (rememberCb != null) {
                rememberCb.setChecked(true);
            }
            String savedUser = cycredit.io.util.UserPrefs.savedUsername(this);
            String savedPass = cycredit.io.util.UserPrefs.savedPassword(this);
            if (!savedUser.isEmpty()) {
                usernameEditText.setText(savedUser);
            }
            if (!savedPass.isEmpty()) {
                passwordEditText.setText(savedPass);
            }
        }

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

        loginButton.setOnClickListener(v -> {
            Log.e("LOGIN_VOLLEY", "=== LOGIN BUTTON CLICKED ===");
            String userTyped = usernameEditText.getText().toString().trim();
            String emailId = toEmailId(userTyped);
            String password = passwordEditText.getText().toString().trim();
            
            Log.d("LOGIN_VOLLEY", "User typed: " + (userTyped.isEmpty() ? "[empty]" : userTyped));
            Log.d("LOGIN_VOLLEY", "Email ID: " + (emailId.isEmpty() ? "[empty]" : emailId));
            Log.d("LOGIN_VOLLEY", "Password length: " + password.length());

            if (userTyped.isEmpty() || password.isEmpty()) {
                Log.w("LOGIN_VOLLEY", "Username or password empty - showing dialog");
                showDialog("Missing info", "Please enter username and password.", null);
                return;
            }

            Log.d("LOGIN_VOLLEY", "Calling performLogin...");
            performLogin(emailId, password, rememberCb != null && rememberCb.isChecked(), userTyped);
        });

        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        deleteUserButton.setOnClickListener(v -> {
            String userTyped = usernameEditText.getText().toString().trim();
            if (userTyped.isEmpty()) {
                showDialog("Error", "Enter your username before deleting.", null);
                return;
            }
            String emailId = toEmailId(userTyped);
            showDeleteDialog(emailId);
        });

        changePassButton.setOnClickListener(v -> showChangePasswordDialog());
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
    
    private boolean isNumericId(String raw) {
        if (raw == null) return false;
        String s = raw.trim();
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
    
    private String usersByEmailUrl(String email) {
        return android.net.Uri.parse(BASE_URL)
                .buildUpon()
                .appendEncodedPath("users")
                .appendQueryParameter("emailId", email)
                .build()
                .toString();
    }
    
    private String usersByIdUrl(int id) {
        return android.net.Uri.parse(BASE_URL)
                .buildUpon()
                .appendEncodedPath("users/" + id)
                .build()
                .toString();
    }

    private String toEmailId(String userTyped) {
        return normalizeUsername(userTyped);
    }

    private void performLogin(String email, String password, boolean rememberMe, String userTyped) {
        // Check if input is numeric ID
        if (isNumericId(userTyped)) {
            int numericId = Integer.parseInt(userTyped.trim());
            performLoginById(numericId, password, rememberMe, userTyped);
            return;
        }
        
        // Otherwise treat as email/username
        String normalizedEmail = normalizeUsername(userTyped);
        String url = usersByEmailUrl(normalizedEmail);
        Log.d("LOGIN_VOLLEY", "GET " + url);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("LOGIN_VOLLEY", "✅ Response received: " + response.toString());

                    String actualPassword = response.optString("password", "");
                    int userId = response.optInt("id", -1);
                    String name = response.optString("name", "Unknown");

                    Log.d("LOGIN_VOLLEY", "Parsed data: userId=" + userId + ", name=" + name + ", passwordLength=" + 
                        (actualPassword != null ? actualPassword.length() : 0));

                    // Validate userId before proceeding - backend should always return valid id
                    if (userId <= 0) {
                        Log.e("LOGIN_VOLLEY", "❌ Invalid userId from server: " + userId);
                        Log.e("LOGIN_VOLLEY", "Full response JSON: " + response.toString());
                        showDialog("Login Failed", "Server returned invalid user data (no user ID). Please contact support.", null);
                        return;
                    }

                    if (!password.equals(actualPassword)) {
                        Log.e("LOGIN_VOLLEY", "❌ Password mismatch");
                        showDialog("Login Failed", "Incorrect password.", null);
                        return;
                    }

                    // Double-check that we have valid user data before proceeding
                    if (name == null || name.trim().isEmpty()) {
                        name = "User"; // Fallback name
                        Log.w("LOGIN_VOLLEY", "Name was empty, using fallback: 'User'");
                    }

                    Session.setUser(getApplicationContext(), userId, name);

                    // Persist backend user ID
                    try {
                        int backendUserId = -1;
                        if (response.has("id")) {
                            backendUserId = response.getInt("id");
                        }
                        // Fallback: if nested
                        if (backendUserId == -1 && response.has("user")) {
                            org.json.JSONObject u = response.getJSONObject("user");
                            if (u.has("id")) {
                                backendUserId = u.getInt("id");
                            }
                        }
                        if (backendUserId != -1) {
                            cycredit.io.util.UserPrefs.saveUserId(LoginActivity.this, backendUserId);
                        }
                    } catch (Exception e) {
                        // If parsing fails, use the userId we already extracted
                        if (userId > 0) {
                            cycredit.io.util.UserPrefs.saveUserId(LoginActivity.this, userId);
                        }
                    }

                    // Save or clear login credentials based on remember me
                    if (rememberMe) {
                        cycredit.io.util.UserPrefs.setRememberMe(LoginActivity.this, true);
                        cycredit.io.util.UserPrefs.saveLogin(LoginActivity.this, userTyped, password);
                    } else {
                        cycredit.io.util.UserPrefs.setRememberMe(LoginActivity.this, false);
                        cycredit.io.util.UserPrefs.saveLogin(LoginActivity.this, "", "");
                    }

                    Log.d("LOGIN_VOLLEY", "✅ Login successful - userId: " + userId + ", name: " + name);
                    
                    // Use emailId from response if available, otherwise use normalized email
                    String finalEmail = response.optString("emailId", email);
                    if (finalEmail == null || finalEmail.isEmpty()) {
                        finalEmail = email;
                    }
                    
                    navigateToMain(userId, finalEmail, name);
                },
                error -> {
                    String errorMessage = "No account found with this email.";
                    
                    // Try to extract error message from response
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String body = new String(error.networkResponse.data, java.nio.charset.StandardCharsets.UTF_8);
                            org.json.JSONObject json = new org.json.JSONObject(body);
                            String serverMessage = json.optString("message", null);
                            if (serverMessage != null && !serverMessage.isEmpty()) {
                                errorMessage = serverMessage;
                            } else {
                                String errorCode = json.optString("error", null);
                                if (errorCode != null) {
                                    errorMessage = "Error: " + errorCode;
                                }
                            }
                        } catch (Exception e) {
                            // Fall back to default message
                            if (error.networkResponse.statusCode == 404) {
                                errorMessage = "No account found with this email.";
                            } else if (error.networkResponse.statusCode == 401) {
                                errorMessage = "Invalid credentials. Please check your email and password.";
                            } else {
                                errorMessage = "Login failed. HTTP " + error.networkResponse.statusCode;
                            }
                        }
                    } else if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                        errorMessage = error.getMessage();
                    }
                    
                    Log.e("LOGIN_VOLLEY", "ERROR: " + errorMessage);
                    showDialog("Login Failed", errorMessage, null);
                }
        );

        req.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        requestQueue.add(req);
    }
    
    private void performLoginById(int userId, String password, boolean rememberMe, String userTyped) {
        String url = usersByIdUrl(userId);
        Log.d("LOGIN_VOLLEY", "GET " + url + " (numeric ID login)");
        
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("LOGIN_VOLLEY", "✅ Response received: " + response.toString());
                    
                    String actualPassword = response.optString("password", "");
                    int backendUserId = response.optInt("id", -1);
                    String name = response.optString("name", "Unknown");
                    String emailId = response.optString("emailId", "");
                    
                    if (backendUserId <= 0) {
                        Log.e("LOGIN_VOLLEY", "❌ Invalid userId from server: " + backendUserId);
                        showDialog("Login Failed", "Server returned invalid user data.", null);
                        return;
                    }
                    
                    if (!password.equals(actualPassword)) {
                        Log.e("LOGIN_VOLLEY", "❌ Password mismatch");
                        showDialog("Login Failed", "Incorrect password.", null);
                        return;
                    }
                    
                    if (name == null || name.trim().isEmpty()) {
                        name = "User";
                    }
                    
                    Session.setUser(getApplicationContext(), backendUserId, name);
                    cycredit.io.util.UserPrefs.saveUserId(LoginActivity.this, backendUserId);
                    
                    if (rememberMe) {
                        cycredit.io.util.UserPrefs.setRememberMe(LoginActivity.this, true);
                        cycredit.io.util.UserPrefs.saveLogin(LoginActivity.this, userTyped, password);
                    } else {
                        cycredit.io.util.UserPrefs.setRememberMe(LoginActivity.this, false);
                        cycredit.io.util.UserPrefs.saveLogin(LoginActivity.this, "", "");
                    }
                    
                    // Use normalized email if available, otherwise construct from username
                    String finalEmail = (emailId != null && !emailId.isEmpty()) 
                            ? emailId 
                            : normalizeUsername(userTyped);
                    
                    navigateToMain(backendUserId, finalEmail, name);
                },
                error -> {
                    String errorMessage = "No account found with this ID.";
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        errorMessage = "No account found with ID " + userId;
                    } else if (error.networkResponse != null) {
                        errorMessage = "Login failed. HTTP " + error.networkResponse.statusCode;
                    }
                    Log.e("LOGIN_VOLLEY", "ERROR: " + errorMessage);
                    showDialog("Login Failed", errorMessage, null);
                }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        requestQueue.add(req);
    }
    
    private void navigateToMain(int userId, String email, String name) {
        try {
            Intent next = new Intent(LoginActivity.this, MainActivity.class);
            int persistedUserId = cycredit.io.util.UserPrefs.userId(LoginActivity.this);
            int finalUserId = persistedUserId > 0 ? persistedUserId : userId;
            
            next.putExtra("EMAIL", email);
            next.putExtra("USER_ID", finalUserId);
            next.putExtra("USERNAME", name);
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            startActivity(next);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                finish();
            }, 500);
        } catch (Exception e) {
            Log.e("LOGIN_VOLLEY", "❌ CRITICAL ERROR starting MainActivity", e);
            showDialog("Navigation Error", "Failed to open main screen: " + e.getMessage(), null);
        }
    }

    private void showDeleteDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete this account?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> fetchAndDeleteUser(email))
                .show();
    }

    private void fetchAndDeleteUser(String email) {
        // URL-encode email to handle special characters
        String encodedEmail;
        try {
            encodedEmail = java.net.URLEncoder.encode(email, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e("LOGIN_VOLLEY", "Failed to encode email: " + e.getMessage());
            encodedEmail = email; // Fallback to unencoded email
        }
        
        String url = BASE_URL + "/users?emailId=" + encodedEmail;
        JsonObjectRequest getReq = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    int id = response.optInt("id", -1);
                    if (id > 0) {
                        deleteUserById(id);
                    } else {
                        showDialog("Delete Failed", "User not found.", null);
                    }
                },
                error -> {
                    String errorMsg = cycredit.io.util.ErrorHandler.getErrorMessage(error);
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Failed to find user. Please check the email address.";
                    }
                    showDialog("Error", errorMsg, null);
                }
        );
        requestQueue.add(getReq);
    }

    private void deleteUserById(int id) {
        String url = BASE_URL + "/users?id=" + id;
        StringRequest delReq = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> showDialog("Deleted", "Account deleted successfully.", null),
                error -> {
                    String errorMsg = cycredit.io.util.ErrorHandler.getErrorMessage(error);
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        if (error.networkResponse != null) {
                            errorMsg = "Delete failed. HTTP " + error.networkResponse.statusCode;
                        } else {
                            errorMsg = "Delete failed. Please try again.";
                        }
                    }
                    showDialog("Delete Failed", errorMsg, null);
                }
        );
        delReq.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        requestQueue.add(delReq);
    }

    private void showChangePasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText oldPass = view.findViewById(R.id.old_pass_edt);
        EditText newPass = view.findViewById(R.id.new_pass_edt);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Change", (d, w) -> {
                    String userTyped = usernameEditText.getText().toString().trim();
                    if (userTyped.isEmpty()) {
                        showDialog("Error", "Enter your username first.", null);
                        return;
                    }
                    String emailId = toEmailId(userTyped);
                    performChangePassword(emailId, oldPass.getText().toString(), newPass.getText().toString());
                })
                .show();
    }

    private void performChangePassword(String email, String oldPass, String newPass) {
        try {
            JSONObject body = new JSONObject();
            body.put("emailId", email);
            body.put("password", oldPass);

            String url = BASE_URL + "/users?oldPassword=" + oldPass + "&newPassword=" + newPass;
            JsonObjectRequest putReq = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    body,
                    response -> showDialog("Success", "Password changed successfully.", null),
                    error -> {
                        String errorMsg = cycredit.io.util.ErrorHandler.getErrorMessage(error);
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            if (error.networkResponse != null) {
                                errorMsg = "Password change failed. HTTP " + error.networkResponse.statusCode;
                            } else {
                                errorMsg = "Password change failed. Please check your old password and try again.";
                            }
                        }
                        showDialog("Error", errorMsg, null);
                    }
            );
            putReq.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
            requestQueue.add(putReq);
        } catch (Exception e) {
            showDialog("Error", e.toString(), null);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) requestQueue.cancelAll(request -> true);
    }
}
