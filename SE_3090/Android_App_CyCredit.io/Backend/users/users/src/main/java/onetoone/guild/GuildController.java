package onetoone.guild;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class GuildController {

    @Autowired private GuildRepository guildRepo;
    @Autowired private GuildMembershipRepository membershipRepo;
    @Autowired private GuildInviteRepository inviteRepo;
    @Operation(
            summary = "Create a new guild",
            description = "Creates a new guild with the given name and assigns the creator as the guild leader."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guild created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/guilds")
    @Transactional
    public ResponseEntity<?> createGuild(@RequestBody CreateGuildRequest req) {
        if (req.getName()==null || req.getName().trim().isEmpty() || req.getCreatorUserId()==null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing name or creatorUserId");
        }
        Guild g = new Guild();
        g.setName(req.getName().trim());
        g.setCreatedBy(req.getCreatorUserId());
        g = guildRepo.save(g);

        GuildMembership m = new GuildMembership();
        m.setGuild(g);
        m.setUserId(req.getCreatorUserId());
        m.setRole(GuildRole.LEADER);
        membershipRepo.save(m);

        return ResponseEntity.ok(new Dtos.GuildDto(g.getId(), g.getName(), g.getCreatedBy(), g.getCreatedAt(),
                membershipRepo.countByGuildId(g.getId()).intValue()));
    }
    @Operation(
            summary = "List all guilds",
            description = "Returns a list of all guilds. Optionally filter by name using the 'q' parameter."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guild list returned"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/guilds")
    public List<Dtos.GuildDto> listGuilds(@RequestParam(value="q", required=false) String q) {
        List<Guild> src = (q==null || q.trim().isEmpty())
                ? guildRepo.findAll()
                : guildRepo.findByNameContainingIgnoreCase(q.trim());
        return src.stream()
                .map(g -> new Dtos.GuildDto(g.getId(), g.getName(), g.getCreatedBy(), g.getCreatedAt(),
                        membershipRepo.countByGuildId(g.getId()).intValue()))
                .collect(Collectors.toList());
    }
    @Operation(
            summary = "List members of a guild",
            description = "Returns all members belonging to the specified guild ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of guild members returned"),
            @ApiResponse(responseCode = "404", description = "Guild not found")
    })
    @GetMapping("/guilds/{id}/members")
    public ResponseEntity<?> members(@PathVariable Integer id) {
        Optional<Guild> og = guildRepo.findById(id);
        if (!og.isPresent()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("guild not found");
        List<Dtos.GuildMemberDto> out = membershipRepo.findByGuildId(id).stream()
                .map(m -> new Dtos.GuildMemberDto(m.getUserId(), m.getRole().name(), m.getJoinedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }
    @Operation(
            summary = "Send a guild invite to a user",
            description = "Allows an existing guild member to invite another user to join the guild."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite sent successfully"),
            @ApiResponse(responseCode = "400", description = "Missing receiverUserId"),
            @ApiResponse(responseCode = "403", description = "Sender is not a member of the guild"),
            @ApiResponse(responseCode = "404", description = "Guild not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/guilds/{id}/invites")
    @Transactional
    public ResponseEntity<?> invite(@PathVariable Integer id,
                                    @RequestParam("senderUserId") Integer senderUserId,
                                    @RequestBody InviteRequest req) {
        Optional<Guild> og = guildRepo.findById(id);
        if (!og.isPresent()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("guild not found");
        Guild g = og.get();

        if (!membershipRepo.existsByGuildIdAndUserId(id, senderUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("sender not a member");
        }
        if (req.getReceiverUserId()==null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing receiverUserId");
        }

        GuildInvite inv = new GuildInvite();
        inv.setGuild(g);
        inv.setSenderUserId(senderUserId);
        inv.setReceiverUserId(req.getReceiverUserId());
        inv = inviteRepo.save(inv);

        return ResponseEntity.ok(new Dtos.InviteDto(inv.getId(), g.getId(), inv.getSenderUserId(),
                inv.getReceiverUserId(), inv.getStatus().name().toLowerCase(), inv.getCreatedAt()));
    }
    @Operation(
            summary = "List invites received by a user",
            description = "Returns all guild invites where the specified user is the receiver."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invites returned successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/guilds/me/invites")
    public List<Dtos.InviteDto> myInvites(@RequestParam("receiverUserId") Integer receiverUserId) {
        return inviteRepo.findByReceiverUserId(receiverUserId).stream()
                .map(inv -> new Dtos.InviteDto(inv.getId(), inv.getGuild().getId(), inv.getSenderUserId(),
                        inv.getReceiverUserId(), inv.getStatus().name().toLowerCase(), inv.getCreatedAt()))
                .collect(Collectors.toList());
    }
    @Operation(
            summary = "Act on an invite (accept or decline)",
            description = "Allows the receiver of an invite to accept or decline it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Action completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid action or invite already processed"),
            @ApiResponse(responseCode = "403", description = "Invite does not belong to this user"),
            @ApiResponse(responseCode = "404", description = "Invite not found")
    })
    @PostMapping("/invites/{inviteId}/{action}")
    @Transactional
    public ResponseEntity<?> act(@PathVariable Integer inviteId,
                                 @PathVariable String action,
                                 @RequestParam("receiverUserId") Integer receiverUserId) {
        Optional<GuildInvite> oi = inviteRepo.findById(inviteId);
        if (!oi.isPresent()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("invite not found");
        GuildInvite inv = oi.get();
        if (!inv.getReceiverUserId().equals(receiverUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not your invite");
        }
        if (inv.getStatus()!=InviteStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("already processed");
        }
        if ("accept".equalsIgnoreCase(action)) {
            inv.setStatus(InviteStatus.ACCEPTED);
            inviteRepo.save(inv);
            GuildMembership m = new GuildMembership();
            m.setGuild(inv.getGuild());
            m.setUserId(receiverUserId);
            m.setRole(GuildRole.MEMBER);
            membershipRepo.save(m);
            return ResponseEntity.ok(new Dtos.InviteActionResponse(inv.getId(), "accepted"));
        } else if ("decline".equalsIgnoreCase(action)) {
            inv.setStatus(InviteStatus.DECLINED);
            inviteRepo.save(inv);
            return ResponseEntity.ok(new Dtos.InviteActionResponse(inv.getId(), "declined"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid action");
        }
    }

    @Autowired private UserRepository userRepo;
    @Operation(
            summary = "Send a guild invite using a username",
            description = "Allows a sender to invite a user by their username rather than user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Guild, sender, or receiver not found")
    })
    @PostMapping("/guilds/{id}/invites/by-name")
    @Transactional
    public ResponseEntity<?> inviteByUsername(
            @PathVariable Integer id,
            @RequestBody InviteRequest req) {

        Optional<Guild> og = guildRepo.findById(id);
        if (!og.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("guild not found");
        }
        Guild g = og.get();

        // Validate receiver username
        if (req.getReceiverName() == null || req.getReceiverName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing receiver username");
        }

        // Validate sender ID
        if (req.getSenderUserId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing sender user id");
        }

        User sender = userRepo.findById(req.getSenderUserId()).orElse(null);
        if (sender == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("sender user not found");
        }

        User receiver = userRepo.getByName(req.getReceiverName().trim());
        if (receiver == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("receiver user not found");
        }




        GuildInvite inv = new GuildInvite();
        inv.setGuild(g);
        inv.setSenderUserId(sender.getId());
        inv.setReceiverUserId(receiver.getId());
        inviteRepo.save(inv);

        return ResponseEntity.ok(new Dtos.InviteDto(
                inv.getId(),
                g.getId(),
                inv.getSenderUserId(),
                inv.getReceiverUserId(),
                inv.getStatus().name().toLowerCase(),
                inv.getCreatedAt()
        ));
    }
}

