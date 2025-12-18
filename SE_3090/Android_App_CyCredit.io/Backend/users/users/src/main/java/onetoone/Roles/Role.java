package onetoone.Roles;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import onetoone.Users.User;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "access_level")
    private int accessLevel;

    @OneToOne(mappedBy = "role")
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonBackReference
    private User user;

    public Role() {}

    public Role(String roleName, int accessLevel) {
        this.roleName = roleName;
        this.accessLevel = accessLevel;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public int getAccessLevel() { return accessLevel; }
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}