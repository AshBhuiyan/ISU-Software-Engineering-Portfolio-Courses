package cycredit.io;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView messageText;
    private TextView tvRoleId;
    private NestedScrollView scrollView;

    private Button chooseRoleBtn, deleteUserBtn;

    private static final String BASE_URL = cycredit.io.guilds.ApiClient.BASE_URL;
    private RequestQueue requestQueue;

    private int userId;
    private int roleId = -1;
    private String email;
    private Integer selectedRoleId = null;
    private String selectedRoleName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log BEFORE anything - even before super.onCreate()
        android.util.Log.e("MainActivity", "========================================");
        android.util.Log.e("MainActivity", "=== MainActivity.onCreate() CALLED ===");
        android.util.Log.e("MainActivity", "========================================");
        
        try {
            android.util.Log.d("MainActivity", "Step 0: Calling super.onCreate()...");
            super.onCreate(savedInstanceState);
            android.util.Log.d("MainActivity", "Step 0: super.onCreate() completed");
            
            android.util.Log.d("MainActivity", "=== onCreate() START ===");
            
            // Log intent extras immediately to debug navigation issues
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                android.util.Log.d("MainActivity", "Intent extras received:");
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    android.util.Log.d("MainActivity", "  EXTRA " + key + " = " + String.valueOf(value));
                }
            } else {
                android.util.Log.w("MainActivity", "No extras on intent!");
            }
            android.util.Log.d("MainActivity", "Step 1: Setting content view...");
            setContentView(R.layout.activity_main);
            android.util.Log.d("MainActivity", "Step 1: Content view set successfully");

            // ---- SESSION / EXTRAS VALIDATION FIRST ----
            android.util.Log.d("MainActivity", "Step 2: Extracting intent extras...");
            userId = getIntent().getIntExtra("USER_ID", -1);
            email  = getIntent().getStringExtra("EMAIL");
            String name  = getIntent().getStringExtra("USERNAME");
            
            // Fallback to prefs if intent extra is missing
            if (userId <= 0) {
                userId = cycredit.io.util.UserPrefs.userId(this);
            }
            
            android.util.Log.d("MainActivity", "Extracted: userId=" + userId + ", email=" + email + ", name=" + name);
            
            // Validate userId BEFORE doing anything else
            if (userId <= 0) {
                android.util.Log.e("MainActivity", "CRITICAL: Invalid userId: " + userId);
                Toast.makeText(this, "Invalid session (no userId). Please login again.", Toast.LENGTH_LONG).show();
                redirectToLogin();
                return;
            }

            // ---- VIEW LOOKUPS (NULL-SAFE) ----
            android.util.Log.d("MainActivity", "Step 3: Finding views...");
            // messageText view removed from layout - using null-safe approach
            messageText   = null; // Layout no longer has main_msg_txt, messages shown via Toast
            tvRoleId      = findViewById(R.id.tv_role_id);
            // Layout uses NestedScrollView, so variable must be NestedScrollView type (not ScrollView)
            scrollView    = findViewById(R.id.scroll_container);
            chooseRoleBtn = findViewById(R.id.choose_role_btn);
            deleteUserBtn = findViewById(R.id.delete_usr_btn);

            // Guard every usage to avoid NPEs
            if (scrollView == null || chooseRoleBtn == null || deleteUserBtn == null) {
                android.util.Log.e("MainActivity", "CRITICAL: UI error - layout IDs not found");
                android.util.Log.e("MainActivity", "  scrollView=" + scrollView + 
                    ", chooseRoleBtn=" + chooseRoleBtn + ", deleteUserBtn=" + deleteUserBtn);
                Toast.makeText(this, "UI error: layout IDs not found. Check activity_main.xml.", Toast.LENGTH_LONG).show();
                // Do NOT redirect; stay and show the error so user can see what's wrong
                return;
            }
            android.util.Log.d("MainActivity", "Step 3: All critical views found successfully");

            // Initialize request queue
            android.util.Log.d("MainActivity", "Step 4: Initializing Volley queue...");
            requestQueue  = Volley.newRequestQueue(this);

            // Setup optional views - Choose Character button
            View chooseCharBtn = findViewById(R.id.btn_choose_character);
            if (chooseCharBtn != null) {
                chooseCharBtn.setClickable(true);
                chooseCharBtn.setEnabled(true);
                chooseCharBtn.setOnClickListener(v -> {
                    // require a role to be chosen (use prefs as source of truth)
                    int roleIdFromPrefs = cycredit.io.util.UserPrefs.roleId(this);
                    String roleNameFromPrefs = cycredit.io.util.UserPrefs.roleName(this);
                    if (roleNameFromPrefs == null || roleNameFromPrefs.trim().isEmpty()) {
                        android.widget.Toast.makeText(this, "Please choose a role first.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.content.Intent i = new android.content.Intent(this, CharacterSelectionActivity.class);
                    int finalUserId = cycredit.io.util.UserPrefs.userId(this);
                    if (finalUserId <= 0) finalUserId = userId;
                    i.putExtra("USER_ID", finalUserId);
                    i.putExtra("EMAIL", email);
                    String username = getIntent().getStringExtra("USERNAME");
                    if (username != null) {
                        i.putExtra("USERNAME", username);
                    }
                    i.putExtra("ROLE_ID", roleIdFromPrefs);
                    i.putExtra("ROLE_NAME", roleNameFromPrefs);
                    startActivity(i);
                });
            }

            // Setup button listeners
            android.util.Log.d("MainActivity", "Step 5: Setting up button listeners...");
            deleteUserBtn.setEnabled(userId > 0);
            chooseRoleBtn.setOnClickListener(v -> showRolePicker());
            deleteUserBtn.setOnClickListener(v -> {
                if (isTaskRoot()) {
                    // Go to login if this is the root (prevents dead-end and crashes)
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                } else {
                    finish();
                }
            });

            // Auto-assign Customer role on startup (only if userId is valid)
            android.util.Log.d("MainActivity", "Step 6: Auto-assigning role...");
            if (userId > 0) {
                // Pre-seed defaults if not already set
                if (cycredit.io.util.UserPrefs.roleName(this).equals("Customer") && 
                    cycredit.io.util.UserPrefs.roleId(this) == 51) {
                    // Only set if still at defaults (not already customized)
                    cycredit.io.util.UserPrefs.saveRole(this, "Customer", 51);
                    cycredit.io.util.UserPrefs.saveCharacter(this, "Cy", -1);
                }
                postUserRole(userId, "Customer");
            }

            // Show welcome message
            android.util.Log.d("MainActivity", "Step 7: Showing welcome message...");
            showWelcomeMessage();
            
            // Update header from saved preferences
            updateHeaderFromPrefs();
            
            android.util.Log.d("MainActivity", "=== onCreate() COMPLETE SUCCESS ===");
            
        } catch (Throwable t) {
            // Catch EVERYTHING - RuntimeException, Error, etc.
            android.util.Log.e("MainActivity", "=== onCreate() CRASH ===", t);
            android.util.Log.e("MainActivity", "Exception type: " + t.getClass().getName());
            android.util.Log.e("MainActivity", "Exception message: " + t.getMessage());
            if (t.getCause() != null) {
                android.util.Log.e("MainActivity", "Caused by: " + t.getCause().getClass().getName() + " - " + t.getCause().getMessage());
            }
            
            // Show visible error to user
            String errorMsg = "Main screen error: " + t.getClass().getSimpleName();
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                errorMsg += "\n" + t.getMessage();
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            
            // Do NOT auto-redirect; let the user see the error and stacktrace in Logcat
            // This way we can identify the exact crash point
        }
    }
    
    private void redirectToLogin() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
        finish();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.e("MainActivity", "=== onStart() CALLED ===");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.e("MainActivity", "=== onResume() CALLED ===");
        // Refresh header when coming back from Role/Character pick
        updateHeaderFromPrefs();
        // Always refresh user ID from backend when returning to this screen
        int uid = cycredit.io.util.UserPrefs.userId(this);
        if (uid <= 0) {
            // Fallback to intent extra if prefs don't have it
            uid = userId;
        }
        cycredit.io.data.UserRepository.refreshUserFromServer(this, uid, this::updateHeaderFromPrefs);
        // Also refresh role to ensure Role ID matches backend
        if (userId > 0) {
            cycredit.io.data.RoleRepository.refreshRoleFromServer(this, userId, this::updateHeaderFromPrefs);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.e("MainActivity", "=== onPause() CALLED ===");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        android.util.Log.e("MainActivity", "=== onStop() CALLED ===");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.e("MainActivity", "=== onDestroy() CALLED ===");
    }
    
    @Override
    public void onBackPressed() {
        // Since we launched with CLEAR_TASK|NEW_TASK, there's no back stack
        // Back button should just exit the app - no redirect needed
        android.util.Log.e("MainActivity", "=== onBackPressed() CALLED ===");
        super.onBackPressed(); // Normal back behavior (will exit since no back stack)
    }

    // -------------------- Welcome --------------------
    private void showWelcomeMessage() {
        // Welcome message is now in the layout XML, just ensure role ID is set
        if (tvRoleId != null && roleId > 0) {
            tvRoleId.setText("Role ID: " + roleId);
        } else if (tvRoleId != null) {
            tvRoleId.setText("Role ID: 0");
        }
        chooseRoleBtn.setVisibility(Button.VISIBLE);
    }

    // -------------------- Header Update from Prefs --------------------
    private void updateHeaderFromPrefs() {
        TextView roleTv = findViewById(R.id.tv_default_role);
        TextView charTv = findViewById(R.id.tv_default_character);
        TextView idTv = findViewById(R.id.tv_role_id);
        
        String roleName = cycredit.io.util.UserPrefs.roleName(this);
        String charName = cycredit.io.util.UserPrefs.characterName(this);
        int userIdPref = cycredit.io.util.UserPrefs.userId(this);
        
        if (roleTv != null) roleTv.setText("Role: " + roleName);
        if (charTv != null) charTv.setText("Character: " + charName);
        if (idTv != null) idTv.setText("User ID: " + (userIdPref > 0 ? userIdPref : "—"));
    }

    // -------------------- Role Picker --------------------
    private void showRolePicker() {
        final String[] roles = {"Customer", "Admin", "Admin+"};
        new AlertDialog.Builder(this)
                .setTitle("Choose your role")
                .setItems(roles, (d, which) -> {
                    String chosenRole = roles[which];
                    if (chosenRole.equals("Admin+")) {
                        promptAdminPlusKey();
                    } else {
                        updateUserRole(chosenRole);
                        // Stay on screen - don't auto-navigate
                    }
                })
                .show();
    }

    private void promptAdminPlusKey() {
        final EditText keyInput = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Enter Developer Key")
                .setMessage("Access restricted to Admin+ members.")
                .setView(keyInput)
                .setPositiveButton("Submit", (d, w) -> {
                    String key = keyInput.getText().toString().trim();
                    if (key.equals("CYDEV2025")) {
                        updateUserRole("Admin+");
                        // Stay on screen - don't auto-navigate
                    } else {
                        Toast.makeText(this, "❌ Invalid key.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // -------------------- CRUD METHODS --------------------
    private void postUserRole(int userId, String role) {
        if (userId <= 0) {
            android.util.Log.w("MainActivity", "postUserRole called with invalid userId: " + userId);
            return;
        }
        
        String url = BASE_URL + "/role/" + userId + "/" + role;
        StringRequest postReq = new StringRequest(
                Request.Method.POST, url,
                response -> {
                    android.util.Log.d("MainActivity", "Role assigned successfully: " + response);
                    Toast.makeText(this, "✅ Role assigned: " + role, Toast.LENGTH_SHORT).show();
                    
                    // Try to parse server role ID from response
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(response);
                        int serverRoleId = -1;
                        if (obj.has("roleId"))      serverRoleId = obj.getInt("roleId");
                        else if (obj.has("id"))     serverRoleId = obj.getInt("id");
                        else if (obj.has("role_id"))serverRoleId = obj.getInt("role_id");
                        
                        // Fallback: if server returns a nested "role" object { id, name }
                        if (serverRoleId == -1 && obj.has("role")) {
                            org.json.JSONObject r = obj.getJSONObject("role");
                            if (r.has("id")) serverRoleId = r.getInt("id");
                        }
                        
                        // Persist server truth (prefer this over any local/default id)
                        if (serverRoleId != -1) {
                            cycredit.io.util.UserPrefs.saveRole(this, role, serverRoleId);
                            updateHeaderFromPrefs();
                        } else {
                            // If server didn't return an id, refresh from server
                            cycredit.io.data.RoleRepository.refreshRoleFromServer(this, userId, this::updateHeaderFromPrefs);
                        }
                    } catch (Exception e) {
                        // Response might not be JSON, refresh role from server
                        android.util.Log.d("MainActivity", "POST response not JSON, refreshing role: " + e.getMessage());
                        cycredit.io.data.RoleRepository.refreshRoleFromServer(this, userId, this::updateHeaderFromPrefs);
                    }
                },
                error -> {
                    String errorMsg = "❌ Role POST failed";
                    if (error.networkResponse != null) {
                        errorMsg += " (HTTP " + error.networkResponse.statusCode + ")";
                        // Don't show error for 400 if it's just "user not found" - might be a timing issue
                        if (error.networkResponse.statusCode == 400) {
                            android.util.Log.w("MainActivity", "Role assignment failed: " + error.getMessage());
                            // Still try to get role in case it was already assigned
                            getUserRole(userId);
                            return;
                        }
                    }
                    android.util.Log.e("MainActivity", errorMsg + ": " + error.toString());
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
        );
        postReq.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        requestQueue.add(postReq);
    }

    private void getUserRole(int userId) {
        if (userId <= 0) {
            android.util.Log.w("MainActivity", "getUserRole called with invalid userId: " + userId);
            return;
        }
        
        String url = BASE_URL + "/role/" + userId;
        StringRequest getReq = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    try {
                        // Handle case where response might be empty or error message
                        if (response == null || response.trim().isEmpty()) {
                            android.util.Log.w("MainActivity", "Empty response from role endpoint");
                            return;
                        }
                        
                        JSONObject json = new JSONObject(response);
                        if (json.has("id")) {
                            roleId = json.getInt("id");
                            String roleName = json.optString("roleName", "Customer");

                            // Save role to preferences
                            cycredit.io.util.UserPrefs.saveRole(MainActivity.this, roleName, roleId);

                            // Update role ID TextView
                            if (tvRoleId != null) {
                                tvRoleId.setText("Role ID: " + roleId);
                            }

                            // Store selected role if we just updated/created it
                            if (selectedRoleName != null && selectedRoleName.equals(roleName)) {
                                selectedRoleId = roleId;
                                updateRoleUI(roleName, roleId);
                            } else {
                                // Update header from prefs to reflect saved role
                                updateHeaderFromPrefs();
                            }
                            scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
                            Toast.makeText(this, "✅ Role fetched: " + roleName, Toast.LENGTH_SHORT).show();
                        } else {
                            // Role doesn't exist yet, that's okay
                            android.util.Log.d("MainActivity", "No role found for user, will be created on first assignment");
                            // Set default role ID to 0
                            if (tvRoleId != null) {
                                tvRoleId.setText("Role ID: 0");
                            }
                        }

                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Parse error in getUserRole: " + e.getMessage(), e);
                        // Don't show error toast for missing role - it's normal on first login
                        if (!response.contains("not found") && !response.contains("404")) {
                            Toast.makeText(this, "⚠️ Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            if (messageText != null) {
                                messageText.setText("⚠️ Failed to parse role info.");
                            }
                        }
                    }
                },
                error -> {
                    // Don't show error for 404 - role might not exist yet
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        android.util.Log.d("MainActivity", "Role not found for user (this is normal on first login)");
                        return;
                    }
                    String errMsg = "❌ Role GET failed: " + (error.getMessage() != null ? error.getMessage() : error.toString());
                    android.util.Log.e("MainActivity", errMsg);
                    Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                    if (messageText != null) {
                        messageText.setText(errMsg);
                    }
                }
        );
        requestQueue.add(getReq);
    }

    private void updateUserRole(String newRole) {
        // Store selected role immediately when user picks it
        selectedRoleName = newRole;
        
        if (roleId == -1) {
            // No role exists yet, create it via POST
            Toast.makeText(this, "Creating role...", Toast.LENGTH_SHORT).show();
            // Save role to preferences immediately (will update ID when fetched)
            cycredit.io.util.UserPrefs.saveRole(this, newRole, 51); // temporary ID, will update when fetched
            postUserRole(userId, newRole);
            // Update header immediately
            updateHeaderFromPrefs();
            return;
        }
        // Role exists, update it via PUT
        String url = BASE_URL + "/role/" + roleId + "/" + newRole;
        StringRequest putReq = new StringRequest(
                Request.Method.PUT, url,
                response -> {
                    Toast.makeText(this, "✅ Role updated: " + newRole, Toast.LENGTH_SHORT).show();
                    
                    // Try to parse server role ID from response
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(response);
                        int serverRoleId = -1;
                        if (obj.has("roleId"))      serverRoleId = obj.getInt("roleId");
                        else if (obj.has("id"))     serverRoleId = obj.getInt("id");
                        else if (obj.has("role_id"))serverRoleId = obj.getInt("role_id");
                        
                        // Fallback: if server returns a nested "role" object { id, name }
                        if (serverRoleId == -1 && obj.has("role")) {
                            org.json.JSONObject r = obj.getJSONObject("role");
                            if (r.has("id")) serverRoleId = r.getInt("id");
                        }
                        
                        // Persist server truth (prefer this over any local/default id)
                        if (serverRoleId != -1) {
                            selectedRoleId = serverRoleId;
                            cycredit.io.util.UserPrefs.saveRole(this, newRole, serverRoleId);
                            updateHeaderFromPrefs();
                        } else {
                            // If server didn't return an id, use current roleId temporarily, then refresh from server
                            selectedRoleId = roleId;
                            cycredit.io.util.UserPrefs.saveRole(this, newRole, roleId);
                            // Refresh to get server's actual ID
                            cycredit.io.data.RoleRepository.refreshRoleFromServer(this, userId, this::updateHeaderFromPrefs);
                        }
                    } catch (Exception e) {
                        // Response might not be JSON, use current roleId temporarily, then refresh
                        android.util.Log.d("MainActivity", "PUT response not JSON, refreshing role: " + e.getMessage());
                        selectedRoleId = roleId;
                        cycredit.io.util.UserPrefs.saveRole(this, newRole, roleId);
                        cycredit.io.data.RoleRepository.refreshRoleFromServer(this, userId, this::updateHeaderFromPrefs);
                    }
                    
                    // Update UI and enable CHOOSE CHARACTER button
                    updateRoleUI(newRole, selectedRoleId != -1 ? selectedRoleId : roleId);
                    // Update header from prefs
                    updateHeaderFromPrefs();
                    getUserRole(userId);
                },
                error -> Toast.makeText(this, "❌ Role PUT failed: " + error.toString(), Toast.LENGTH_LONG).show()
        );
        requestQueue.add(putReq);
    }

    private void deleteUserRole() {
        if (roleId == -1) {
            Toast.makeText(this, "No role found.", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = BASE_URL + "/role/" + roleId;
        StringRequest delReq = new StringRequest(
                Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "✅ Role deleted.", Toast.LENGTH_SHORT).show();
                    roleId = -1;
                    if (messageText != null) {
                        messageText.setText("Role removed. You can choose a new one.");
                    }
                },
                error -> Toast.makeText(this, "❌ Role DELETE failed: " + error.toString(), Toast.LENGTH_LONG).show()
        );
        requestQueue.add(delReq);
    }


    private void updateRoleUI(String roleName, int roleId) {
        // Update role ID TextView
        if (tvRoleId != null) {
            tvRoleId.setText("Role ID: " + roleId);
        }
        
        // Enable CHOOSE CHARACTER button
        Button chooseCharacterBtn = findViewById(R.id.btn_choose_character);
        if (chooseCharacterBtn != null) {
            chooseCharacterBtn.setEnabled(true);
        }
    }



    private void showInstructions() {
        String instructions =
                "How to Play CyCredit.io\n\n" +
                        "1) Receive your virtual credit card and starting score.\n" +
                        "2) Each round, choose purchases and payments.\n" +
                        "3) Keep utilization under 30%.\n" +
                        "4) Pay on time to boost your score.\n" +
                        "5) Roles give different privileges.";
        if (messageText != null) {
            messageText.setText(instructions);
        }
    }

    private void goToGame() {
        Intent intent = new Intent(MainActivity.this, CounterActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteUser() {
        new AlertDialog.Builder(this)
                .setTitle("Delete account?")
                .setMessage("This will remove your account and role from the server.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> deleteUserFromServer())
                .show();
    }

    private void deleteUserFromServer() {
        if (userId <= 0) {
            Toast.makeText(this, "Invalid user ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BASE_URL + "/users?id=" + userId;
        StringRequest deleteRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "✅ Deleted user: " + response, Toast.LENGTH_LONG).show();
                    deleteUserRole();
                },
                error -> Toast.makeText(this, "❌ Delete failed: " + error.toString(), Toast.LENGTH_LONG).show()
        );
        deleteRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        requestQueue.add(deleteRequest);
    }
}
