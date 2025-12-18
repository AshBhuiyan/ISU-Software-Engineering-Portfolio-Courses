package onetoone.guild;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="guild_invites")
public class GuildInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="guild_id")
    private Guild guild;

    @Column(nullable=false)
    private Integer senderUserId;

    @Column(nullable=false)
    private Integer receiverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    public Integer getId(){ return id; }
    public Guild getGuild(){ return guild; }
    public void setGuild(Guild guild){ this.guild = guild; }
    public Integer getSenderUserId(){ return senderUserId; }
    public void setSenderUserId(Integer senderUserId){ this.senderUserId = senderUserId; }
    public Integer getReceiverUserId(){ return receiverUserId; }
    public void setReceiverUserId(Integer receiverUserId){ this.receiverUserId = receiverUserId; }
    public InviteStatus getStatus(){ return status; }
    public void setStatus(InviteStatus status){ this.status = status; }
    public Instant getCreatedAt(){ return createdAt; }
    public void setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
}
