package cycredit.io;

public class MapLocationModel {
    private int id;
    private String name;
    private String category; // FINANCE, STORE, etc.
    private float xPercent;  // 0..1 left->right
    private float yPercent;  // 0..1 top->bottom
    private int iconResId;

    public MapLocationModel(int id, String name, String category, float xPercent, float yPercent, int iconResId) {
        this.id = id; this.name = name; this.category = category;
        this.xPercent = xPercent; this.yPercent = yPercent; this.iconResId = iconResId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public float getXPercent() { return xPercent; }
    public float getYPercent() { return yPercent; }
    public int getIconResId() { return iconResId; }
}
