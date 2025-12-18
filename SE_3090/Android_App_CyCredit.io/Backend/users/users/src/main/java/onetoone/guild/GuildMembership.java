package onetoone.guild;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="guild_membership")
public class GuildMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="guild_id")
    private Guild guild;

    @Column(nullable=false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private GuildRole role = GuildRole.MEMBER;

    @Column(nullable=false)
    private Instant joinedAt = Instant.now();

    public Integer getId(){ return id; }
    public Guild getGuild(){ return guild; }
    public void setGuild(Guild guild){ this.guild = guild; }
    public Integer getUserId(){ return userId; }
    public void setUserId(Integer userId){ this.userId = userId; }
    public GuildRole getRole(){ return role; }
    public void setRole(GuildRole role){ this.role = role; }
    public Instant getJoinedAt(){ return joinedAt; }
    public void setJoinedAt(Instant joinedAt){ this.joinedAt = joinedAt; }
}
