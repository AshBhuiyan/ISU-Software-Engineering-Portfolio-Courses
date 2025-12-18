package cycredit.io;

public class CharacterModel {
    private String name;
    private int imageResId;
    private String description;

    public CharacterModel(String name, int imageResId, String description) {
        this.name = name;
        this.imageResId = imageResId;
        this.description = description;
    }

    public String getName() { return name; }
    public int getImageResId() { return imageResId; }
    public String getDescription() { return description; }
}
