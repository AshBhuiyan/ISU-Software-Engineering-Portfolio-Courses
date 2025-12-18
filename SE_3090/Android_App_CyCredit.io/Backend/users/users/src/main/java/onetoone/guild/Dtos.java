package onetoone.guild;

import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public static class GuildDto {
        private Integer id;
        private String name;
        private Integer createdBy;
        private Instant createdAt;
        private Integer memberCount;
        public GuildDto(Integer id, String name, Integer createdBy, Instant createdAt, Integer memberCount){
            this.id=id; this.name=name; this.createdBy=createdBy; this.createdAt=createdAt; this.memberCount=memberCount;
        }
        public Integer getId(){ return id; }
        public String getName(){ return name; }
        public Integer getCreatedBy(){ return createdBy; }
        public Instant getCreatedAt(){ return createdAt; }
        public Integer getMemberCount(){ return memberCount; }
    }

    public static class GuildMemberDto {
        private Integer userId;
        private String role;
        private Instant joinedAt;
        public GuildMemberDto(Integer userId, String role, Instant joinedAt){
            this.userId=userId; this.role=role; this.joinedAt=joinedAt;
        }
        public Integer getUserId(){ return userId; }
        public String getRole(){ return role; }
        public Instant getJoinedAt(){ return joinedAt; }
    }

    public static class InviteDto {
        private Integer id;
        private Integer guildId;
        private Integer senderUserId;
        private Integer receiverUserId;
        private String status;
        private Instant createdAt;
        public InviteDto(Integer id, Integer guildId, Integer senderUserId, Integer receiverUserId,
                         String status, Instant createdAt){
            this.id=id; this.guildId=guildId; this.senderUserId=senderUserId;
            this.receiverUserId=receiverUserId; this.status=status; this.createdAt=createdAt;
        }
        public Integer getId(){ return id; }
        public Integer getGuildId(){ return guildId; }
        public Integer getSenderUserId(){ return senderUserId; }
        public Integer getReceiverUserId(){ return receiverUserId; }
        public String getStatus(){ return status; }
        public Instant getCreatedAt(){ return createdAt; }
    }

    public static class InviteActionResponse {
        private Integer inviteId;
        private String status;
        public InviteActionResponse(Integer inviteId, String status){
            this.inviteId=inviteId; this.status=status;
        }
        public Integer getInviteId(){ return inviteId; }
        public String getStatus(){ return status; }
    }
}
