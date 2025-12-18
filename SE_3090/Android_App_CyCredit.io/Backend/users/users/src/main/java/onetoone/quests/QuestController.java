package onetoone.quests;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/quests")
@CrossOrigin(origins = "*")
public class QuestController {

    private final QuestRepository quests;
    private final UserQuestProgressRepository progressRepo;

    public QuestController(QuestRepository quests, UserQuestProgressRepository progressRepo) {
        this.quests = quests;
        this.progressRepo = progressRepo;
        seedQuestsOnce();
    }

    private void seedQuestsOnce() {
        if (quests.count() == 0) {
            QuestEntity q1 = new QuestEntity();
            q1.questId = "q1"; q1.title = "Welcome Aboard"; q1.description = "Complete your first task"; q1.rewardPoints = 100;
            QuestEntity q2 = new QuestEntity();
            q2.questId = "q2"; q2.title = "Explorer"; q2.description = "Visit 5 sections in the app"; q2.rewardPoints = 250;
            QuestEntity q3 = new QuestEntity();
            q3.questId = "q3"; q3.title = "Finisher"; q3.description = "Complete all onboarding steps"; q3.rewardPoints = 500;
            quests.saveAll(Arrays.asList(q1, q2, q3));
        }
    }
    @Operation(
            summary = "List all quests",
            description = "Returns a list of all available quests with their details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request succeeded"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> listQuests() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (QuestEntity q : quests.findAll()) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("questId", q.questId);
            m.put("title", q.title);
            m.put("description", q.description);
            m.put("rewardPoints", q.rewardPoints);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }
    @Operation(
            summary = "Get user quest progress",
            description = "Returns the current progress of all quests for a given user. Initializes progress if none exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request succeeded"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/progress")
    public ResponseEntity<List<Map<String,Object>>> userProgress(@RequestParam("userId") String userId) {
        List<UserQuestProgressEntity> rows = progressRepo.findByIdUserId(userId);
        if (rows.isEmpty()) {
            List<UserQuestProgressEntity> init = new ArrayList<>();
            for (QuestEntity q : quests.findAll()) {
                UserQuestProgressEntity r = new UserQuestProgressEntity();
                r.id = new UserQuestProgressEntity.Id(userId, q.questId);
                r.status = "IN_PROGRESS";
                r.progressPercent = 0;
                r.completedAtIso  = null;
                init.add(r);
            }
            rows = progressRepo.saveAll(init);
        }
        return ResponseEntity.ok(toDto(rows));
    }
    @Operation(
            summary = "Set progress for a quest",
            description = "Sets the progress percentage for a given quest and user. Updates status automatically."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request succeeded"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{questId}/progress")
    public ResponseEntity<Map<String,Object>> setProgress(
            @PathVariable String questId,
            @RequestParam String userId,
            @RequestParam int percent) {

        if (!quests.existsById(questId)) return ResponseEntity.notFound().build();
        percent = Math.max(0, Math.min(100, percent));

        UserQuestProgressEntity.Id id = new UserQuestProgressEntity.Id(userId, questId);
        UserQuestProgressEntity r = progressRepo.findById(id).orElseGet(() -> {
            UserQuestProgressEntity x = new UserQuestProgressEntity();
            x.id = id; x.status = "IN_PROGRESS"; x.progressPercent = 0; return x;
        });

        r.progressPercent = percent;

        if (percent >= 100) {
            r.status = "COMPLETED";
            r.completedAtIso = java.time.Instant.now().toString();
        } else if (percent > 0) {
            r.status = "IN_PROGRESS";
            r.completedAtIso = null;
        } else {
            r.status = "IN_PROGRESS";
            r.completedAtIso = null;
        }
        progressRepo.save(r);
        maybeCompleteFinisher(userId);

        Map<String,Object> m = new LinkedHashMap<>();
        m.put("questId", r.id.questId);
        m.put("status", r.status);
        m.put("progressPercent", r.progressPercent);
        m.put("completedAtIso", r.completedAtIso);
        return ResponseEntity.ok(m);
    }
    @Operation(
            summary = "Increment progress for a quest",
            description = "Adds a delta to the current progress of a quest for a given user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request succeeded"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{questId}/tick")
    public ResponseEntity<Map<String,Object>> addProgress(
            @PathVariable String questId,
            @RequestParam String userId,
            @RequestParam int delta) {

        if (!quests.existsById(questId)) return ResponseEntity.notFound().build();

        UserQuestProgressEntity.Id id = new UserQuestProgressEntity.Id(userId, questId);
        UserQuestProgressEntity r = progressRepo.findById(id).orElseGet(() -> {
            UserQuestProgressEntity x = new UserQuestProgressEntity();
            x.id = id; x.status = "IN_PROGRESS"; x.progressPercent = 0; return x;
        });

        int next = Math.max(0, Math.min(100, r.progressPercent + delta));
        r.progressPercent = next;
        if (next >= 100) {
            r.status = "COMPLETED";
            r.completedAtIso = java.time.Instant.now().toString();
        } else {
            r.status = "IN_PROGRESS";
            r.completedAtIso = null;
        }
        progressRepo.save(r);
        maybeCompleteFinisher(userId);

        Map<String,Object> m = new LinkedHashMap<>();
        m.put("questId", r.id.questId);
        m.put("status", r.status);
        m.put("progressPercent", r.progressPercent);
        m.put("completedAtIso", r.completedAtIso);
        return ResponseEntity.ok(m);
    }
    @Operation(
            summary = "Mark quest as complete",
            description = "Immediately completes a quest for the given user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request succeeded"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{questId}/complete")
    public ResponseEntity<Map<String,Object>> complete(
            @PathVariable String questId,
            @RequestParam String userId) {

        if (!quests.existsById(questId)) return ResponseEntity.notFound().build();

        UserQuestProgressEntity.Id id = new UserQuestProgressEntity.Id(userId, questId);
        UserQuestProgressEntity r = progressRepo.findById(id).orElseGet(() -> {
            UserQuestProgressEntity x = new UserQuestProgressEntity();
            x.id = id; return x;
        });
        r.status = "COMPLETED"; r.progressPercent = 100; r.completedAtIso = java.time.Instant.now().toString();
        progressRepo.save(r);
        maybeCompleteFinisher(userId);

        Map<String,Object> m = new LinkedHashMap<>();
        m.put("questId", r.id.questId);
        m.put("status", r.status);
        m.put("progressPercent", r.progressPercent);
        m.put("completedAtIso", r.completedAtIso);
        return ResponseEntity.ok(m);
    }

    private List<Map<String,Object>> toDto(List<UserQuestProgressEntity> rows) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (UserQuestProgressEntity r : rows) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("questId", r.id.questId);
            m.put("status", r.status);
            m.put("progressPercent", r.progressPercent);
            m.put("completedAtIso", r.completedAtIso);
            out.add(m);
        }
        return out;
    }

    private void maybeCompleteFinisher(String userId) {
        UserQuestProgressEntity.Id q2id = new UserQuestProgressEntity.Id(userId, "q2");

        progressRepo.findById(q2id).ifPresent(q2row -> {
            if (q2row.progressPercent >= 100) {
                UserQuestProgressEntity.Id q3id = new UserQuestProgressEntity.Id(userId, "q3");
                UserQuestProgressEntity fin = progressRepo.findById(q3id).orElseGet(() -> {
                    UserQuestProgressEntity x = new UserQuestProgressEntity();
                    x.id = q3id;
                    return x;
                });

                fin.status = "COMPLETED";
                fin.progressPercent = 100;
                fin.completedAtIso = java.time.Instant.now().toString();
                progressRepo.save(fin);
            }
        });
    }
}