package onetoone.messaging.chat;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="chat_messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable=false,length=16)
    private String scope; // public | guild | dm

    @Column(nullable=false,length=64)
    private String channel; // "global" | guildId | dmKey

    private Integer fromUserId;

    @Column(length=64)
    private String username;

    @Column(nullable=false,length=2000)
    private String content;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();


    public Integer getId(){return id;}
    public String getScope(){return scope;}
    public void setScope(String s){this.scope=s;}
    public String getChannel(){return channel;}
    public void setChannel(String c){this.channel=c;}
    public Integer getFromUserId(){return fromUserId;}
    public void setFromUserId(Integer v){this.fromUserId=v;}
    public String getUsername(){return username;}
    public void setUsername(String v){this.username=v;}
    public String getContent(){return content;}
    public void setContent(String v){this.content=v;}
    public Instant getCreatedAt(){return createdAt;}
    public void setCreatedAt(Instant t){this.createdAt=t;}
}
