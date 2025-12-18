package onetoone.guild;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="guilds")
public class Guild {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable=false, unique=true)
    private String name;

    @Column(nullable=false)
    private Integer createdBy;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    public Integer getId(){ return id; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public Integer getCreatedBy(){ return createdBy; }
    public void setCreatedBy(Integer createdBy){ this.createdBy = createdBy; }
    public Instant getCreatedAt(){ return createdAt; }
    public void setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
}
