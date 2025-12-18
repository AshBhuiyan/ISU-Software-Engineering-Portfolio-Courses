package onetoone.game;

import onetoone.util.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/state")
    public ResponseEntity<?> getGameState(@RequestParam int userId) {
        try {
            var resource = gameService.getGameState(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("turnsLeft", resource.getTurnsLeft());
            response.put("money", resource.getMoney());
            response.put("creditScore", resource.getCredit());
            response.put("creditLimit", resource.getCreditLimit());
            response.put("currentMonth", resource.getCurrentMonth());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.RESOURCE_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Game state error: " + e.getMessage()));
        }
    }

    @PostMapping("/end-month")
    public ResponseEntity<?> endMonth(@RequestParam int userId) {
        try {
            var summary = gameService.endMonth(userId);
            return ResponseEntity.ok(summary);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.RESOURCE_NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "End month error: " + e.getMessage()));
        }
    }
}
