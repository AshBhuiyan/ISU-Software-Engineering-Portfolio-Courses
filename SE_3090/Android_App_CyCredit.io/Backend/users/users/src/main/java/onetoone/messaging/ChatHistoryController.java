package onetoone.messaging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.messaging.chat.ChatMessage;
import onetoone.messaging.chat.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatHistoryController {
    @Autowired ChatMessageRepository repo;
    @Operation(
            summary = "Fetch recent chat messages",
            description = "Returns the most recent messages for a specific chat channel and scope. " +
                    "The number of messages returned can be limited (max 200)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{scope}/{channel}/history")
    public List<ChatMessage> history(@PathVariable String scope, @PathVariable String channel,
                                     @RequestParam(defaultValue = "50") int limit){
        return repo.recent(scope, channel, PageRequest.of(0, Math.min(Math.max(limit,1), 200)));
    }
}
