package onetoone.Location;

import jakarta.persistence.*;

@Entity
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String category;
    private double xPercent;
    private double yPercent;

    @Column(length = 512)
    private String description;

    private String imageUrl;

    public Location() {}

    public Location(String name, String category, double xPercent, double yPercent, String description) {
        this.name = name;
        this.category = category;
        this.xPercent = xPercent;
        this.yPercent = yPercent;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getXPercent() { return xPercent; }
    public void setXPercent(double xPercent) { this.xPercent = xPercent; }

    public double getYPercent() { return yPercent; }
    public void setYPercent(double yPercent) { this.yPercent = yPercent; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
