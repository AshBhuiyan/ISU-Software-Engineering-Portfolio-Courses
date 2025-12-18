package cycredit.io.model;

import org.json.JSONObject;

public class StoreItem {
    private final int id;
    private final String name;
    private final String description;
    private final double price;
    private final String category;

    public StoreItem(int id, String name, String description, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = "General";
    }
    
    public StoreItem(int id, String name, String description, double price, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category != null ? category : "General";
    }

    public static StoreItem fromJson(JSONObject o) {
        int id = o.optInt("id", -1);
        String name = o.optString("name", "Item");
        String desc = o.optString("description", "");
        double price = o.optDouble("price", 0.0);
        String category = o.optString("category", null);
        // Handle null, empty, or literal "null" string
        if (category == null || category.isEmpty() || "null".equalsIgnoreCase(category)) {
            category = "Other";
        }
        return new StoreItem(id, name, desc, price, category);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
}