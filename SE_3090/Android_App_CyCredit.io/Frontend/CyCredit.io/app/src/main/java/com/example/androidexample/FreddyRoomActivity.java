package cycredit.io;

import android.app.AlertDialog;
import android.text.InputType;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cycredit.io.guilds.ApiClient;
import cycredit.io.util.ErrorHandler;

public class FreddyRoomActivity extends AppCompatActivity {

    private int userId;

    private SwipeRefreshLayout refresher;
    private RoomCanvasView canvasView;
    private LinearLayout inventoryContainer;
    private TextView capText;
    private TextView emptyState;
    private ProgressBar progressBar;

    private final List<Furniture> furnitureList = new ArrayList<>();
    private final List<InventoryItem> inventoryItems = new ArrayList<>();
    private int maxItems = 20;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freddy_room);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Freddy Court – My Room");
        }

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId <= 0) {
            userId = Session.getUserId(this);
        }
        
        // Defensive check for required extras
        if (userId <= 0) {
            Toast.makeText(this, "Missing user data. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupListeners();
        setupBackButton();
        refresher.setRefreshing(true);
        fetchLayout();
    }
    
    private void setupBackButton() {
        android.widget.Button backBtn = findViewById(R.id.btn_back_to_map);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    private void bindViews() {
        refresher = findViewById(R.id.swipe_refresh);
        canvasView = findViewById(R.id.room_canvas);
        inventoryContainer = findViewById(R.id.inventory_container);
        capText = findViewById(R.id.cap_text);
        emptyState = findViewById(R.id.empty_state);
        progressBar = findViewById(R.id.progress_bar);

        canvasView.setOnItemInteractionListener(item -> showFurnitureOptions(item));
    }

    private void setupListeners() {
        refresher.setOnRefreshListener(this::fetchLayout);
        findViewById(R.id.add_item_button).setOnClickListener(v -> showPlaceItemDialog());
    }

    private void fetchLayout() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiClient.BASE_URL + "/home/layout?userId=" + userId,
                null,
                response -> {
                    refresher.setRefreshing(false);
                    parseLayoutResponse(response);
                },
                error -> {
                    refresher.setRefreshing(false);
                    cycredit.io.util.ErrorHandler.handleError(this, error, "Failed to load room");
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void parseLayoutResponse(JSONObject response) {
        furnitureList.clear();
        inventoryItems.clear();

        JSONArray furnitureArray = response.optJSONArray("furniture");
        if (furnitureArray != null) {
            for (int i = 0; i < furnitureArray.length(); i++) {
                JSONObject obj = furnitureArray.optJSONObject(i);
                if (obj == null) continue;
                furnitureList.add(Furniture.fromJson(obj));
            }
        }

        JSONArray inventoryArray = response.optJSONArray("inventory");
        if (inventoryArray != null) {
            for (int i = 0; i < inventoryArray.length(); i++) {
                JSONObject obj = inventoryArray.optJSONObject(i);
                if (obj == null) continue;
                inventoryItems.add(InventoryItem.fromJson(obj));
            }
        }

        maxItems = response.optInt("maxItems", 20);

        updateUi();
    }

    private void updateUi() {
        capText.setText(String.format(Locale.US, "%d / %d items placed", furnitureList.size(), maxItems));
        emptyState.setVisibility(furnitureList.isEmpty() ? View.VISIBLE : View.GONE);
        canvasView.setItems(toCanvasItems());
        renderInventory();
    }

    private List<RoomCanvasView.RoomItem> toCanvasItems() {
        List<RoomCanvasView.RoomItem> items = new ArrayList<>();
        for (Furniture f : furnitureList) {
            items.add(f.toCanvasItem());
        }
        return items;
    }

    private void renderInventory() {
        inventoryContainer.removeAllViews();
        if (inventoryItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Empty inventory - Buy items from Memorial Union!");
            empty.setPadding(24, 12, 24, 12);
            empty.setTextColor(0xFFB0C4D8); // Light gray for visibility
            empty.setTextSize(14f);
            inventoryContainer.addView(empty);
            return;
        }
        for (InventoryItem item : inventoryItems) {
            TextView view = buildInventoryChip(item);
            inventoryContainer.addView(view);
        }
    }

    private TextView buildInventoryChip(InventoryItem item) {
        TextView tv = new TextView(this);
        tv.setText(String.format(Locale.US, "%s (%d)", item.itemCode, item.quantity));
        tv.setPadding(32, 16, 32, 16);
        tv.setBackgroundResource(R.drawable.room_item_bg);
        tv.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        tv.setLayoutParams(params);
        tv.setOnClickListener(v -> showPlacementDialog(item));
        return tv;
    }

    private void showPlaceItemDialog() {
        if (inventoryItems.isEmpty()) {
            Toast.makeText(this, "No inventory items available.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[inventoryItems.size()];
        for (int i = 0; i < inventoryItems.size(); i++) {
            InventoryItem item = inventoryItems.get(i);
            labels[i] = String.format(Locale.US, "%s (x%d)", item.itemCode, item.quantity);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Item to Place")
                .setItems(labels, (dialog, which) -> showPlacementDialog(inventoryItems.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPlacementDialog(InventoryItem item) {
        if (item.quantity <= 0) {
            Toast.makeText(this, "Item out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = buildCoordinateLayout(null);
        new AlertDialog.Builder(this)
                .setTitle("Place " + item.itemCode)
                .setView(layout)
                .setPositiveButton("Place", (dialog, which) -> placeItem(item, extractCoordinates(layout)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void placeItem(InventoryItem item, Coordinates coordinates) {
        if (coordinates == null) return;
        if (furnitureList.size() >= maxItems) {
            Toast.makeText(this, "Room cap reached (20 items).", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        JSONObject body = new JSONObject();
        try {
            body.put("itemCode", item.itemCode);
            body.put("x", coordinates.x);
            body.put("y", coordinates.y);
            body.put("rotation", coordinates.rotation);
            body.put("z", coordinates.z);
        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to build request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiClient.BASE_URL + "/home/place?userId=" + userId,
                body,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    furnitureList.add(Furniture.fromJson(response));
                    decrementInventory(item.itemCode);
                    updateUi();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    cycredit.io.util.ErrorHandler.handleError(this, error, "Place failed");
                    fetchLayout();
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void showFurnitureOptions(RoomCanvasView.RoomItem item) {
        String[] options = {"Move", "Remove", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Furniture Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Furniture furniture = findFurniture(item.id);
                        if (furniture != null) {
                            showMoveDialog(furniture);
                        }
                    } else if (which == 1) {
                        removeFurniture(item.id);
                    }
                })
                .show();
    }

    private void showMoveDialog(Furniture furniture) {
        LinearLayout layout = buildCoordinateLayout(furniture);
        new AlertDialog.Builder(this)
                .setTitle("Move " + furniture.itemCode)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> moveFurniture(furniture.id, extractCoordinates(layout)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void moveFurniture(long id, Coordinates coordinates) {
        if (coordinates == null) return;
        progressBar.setVisibility(View.VISIBLE);

        JSONObject body = new JSONObject();
        try {
            body.put("x", coordinates.x);
            body.put("y", coordinates.y);
            body.put("rotation", coordinates.rotation);
            body.put("z", coordinates.z);
        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to build request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PATCH,
                ApiClient.BASE_URL + "/home/move/" + id + "?userId=" + userId,
                body,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Furniture updated = Furniture.fromJson(response);
                    replaceFurniture(updated);
                    updateUi();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    cycredit.io.util.ErrorHandler.handleError(this, error, "Move failed");
                    fetchLayout();
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private void removeFurniture(long id) {
        progressBar.setVisibility(View.VISIBLE);
        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                ApiClient.BASE_URL + "/home/remove/" + id + "?userId=" + userId,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        parseLayoutResponse(new JSONObject(response));
                    } catch (JSONException e) {
                        fetchLayout();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    cycredit.io.util.ErrorHandler.handleError(this, error, "Remove failed");
                    fetchLayout();
                }
        );
        ApiClient.getRequestQueue(this).add(request);
    }

    private Furniture findFurniture(long id) {
        for (Furniture furniture : furnitureList) {
            if (furniture.id == id) return furniture;
        }
        return null;
    }

    private void replaceFurniture(Furniture furniture) {
        for (int i = 0; i < furnitureList.size(); i++) {
            if (furnitureList.get(i).id == furniture.id) {
                furnitureList.set(i, furniture);
                return;
            }
        }
        furnitureList.add(furniture);
    }

    private void decrementInventory(String itemCode) {
        for (int i = 0; i < inventoryItems.size(); i++) {
            InventoryItem item = inventoryItems.get(i);
            if (item.itemCode.equals(itemCode)) {
                item.quantity -= 1;
                if (item.quantity <= 0) {
                    inventoryItems.remove(i);
                }
                return;
            }
        }
    }

    private LinearLayout buildCoordinateLayout(Furniture furniture) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        layout.addView(buildField("X (0-7)", furniture != null ? furniture.x : 0));
        layout.addView(buildField("Y (0-5)", furniture != null ? furniture.y : 0));
        layout.addView(buildField("Rotation (degrees)", furniture != null ? furniture.rotation : 0));
        layout.addView(buildField("Layer (z)", furniture != null ? furniture.z : 0));
        return layout;
    }

    private View buildField(String label, int defaultValue) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(14f);
        title.setTextColor(0xFF333333); // Dark text for dialog
        title.setPadding(0, 12, 0, 4);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(String.valueOf(defaultValue));
        input.setTag(label);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setTextColor(0xFF000000); // Black text
        input.setHintTextColor(0xFF888888);

        container.addView(title);
        container.addView(input);
        return container;
    }

    private Coordinates extractCoordinates(LinearLayout layout) {
        int x = readInput(layout, 0);
        int y = readInput(layout, 1);
        int rotation = readInput(layout, 2);
        int z = readInput(layout, 3);
        if (x < 0 || x > 7 || y < 0 || y > 5) {
            Toast.makeText(this, "Coordinates out of bounds (grid 8x6).", Toast.LENGTH_SHORT).show();
            return null;
        }
        return new Coordinates(x, y, rotation, z);
    }

    private int readInput(LinearLayout layout, int index) {
        View container = layout.getChildAt(index);
        if (!(container instanceof LinearLayout)) return 0;
        LinearLayout inner = (LinearLayout) container;
        EditText input = (EditText) inner.getChildAt(1);
        try {
            return Integer.parseInt(input.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static class Furniture {
        final long id;
        final String itemCode;
        final int x;
        final int y;
        final int rotation;
        final int z;
        final boolean starter;

        Furniture(long id, String itemCode, int x, int y, int rotation, int z, boolean starter) {
            this.id = id;
            this.itemCode = itemCode;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.z = z;
            this.starter = starter;
        }

        static Furniture fromJson(JSONObject obj) {
            return new Furniture(
                    obj.optLong("id"),
                    obj.optString("itemCode"),
                    obj.optInt("x"),
                    obj.optInt("y"),
                    obj.optInt("rotation"),
                    obj.optInt("z"),
                    obj.optBoolean("starter")
            );
        }

        RoomCanvasView.RoomItem toCanvasItem() {
            return new RoomCanvasView.RoomItem(id, itemCode, x, y, rotation, z, starter);
        }
    }

    private static class InventoryItem {
        final String itemCode;
        int quantity;

        InventoryItem(String itemCode, int quantity) {
            this.itemCode = itemCode;
            this.quantity = quantity;
        }

        static InventoryItem fromJson(JSONObject obj) {
            return new InventoryItem(obj.optString("itemCode"), obj.optInt("quantity"));
        }
    }

    private static class Coordinates {
        final int x;
        final int y;
        final int rotation;
        final int z;

        Coordinates(int x, int y, int rotation, int z) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.z = z;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        int rotation() {
            return rotation;
        }

        int z() {
            return z;
        }
    }
}

