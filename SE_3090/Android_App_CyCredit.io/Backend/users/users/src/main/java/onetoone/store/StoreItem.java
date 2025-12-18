
package onetoone.store;

import jakarta.persistence.*;

@Entity
@Table(name = "store_items")
public class StoreItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String name;
    private String description;
    private double price;
    private String category;
    private String iconName;  // Icon resource name for frontend

    public StoreItem() {}

    public StoreItem(String n, String d, double p) {
        this.name = n;
        this.description = d;
        this.price = p;
    }

    public StoreItem(String n, String d, double p, String cat) {
        this.name = n;
        this.description = d;
        this.price = p;
        this.category = cat;
    }

    public StoreItem(String n, String d, double p, String cat, String icon) {
        this.name = n;
        this.description = d;
        this.price = p;
        this.category = cat;
        this.iconName = icon;
    }
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }


    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
}


