package onetoone.billing;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.game.GameService;
import onetoone.util.ApiError;
import onetoone.util.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/statements")
public class StatementController {

    private static final Logger log = LoggerFactory.getLogger(StatementController.class);

    private final StatementRepository statementRepo;
    private final StatementService statementService;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;
    private final TransactionRepository txRepo;
    private final GameService gameService;

    public StatementController(StatementRepository statementRepo, StatementService statementService,
                              UserRepository userRepo, ResourceRepository resourceRepo,
                              TransactionRepository txRepo, GameService gameService) {
        this.statementRepo = statementRepo;
        this.statementService = statementService;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
        this.txRepo = txRepo;
        this.gameService = gameService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentStatement(@RequestParam int userId) {
        try {
            log.info("Fetching current statement for userId={}", userId);
            Statement statement = statementService.getCurrentStatement(userId);
            if (statement == null) {
                log.info("No current statement found for userId={}", userId);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No current statement");
                return ResponseEntity.ok(response);
            }
            log.info("Found statement for userId={}: id={}, totalDue={}, minimumDue={}, status={}",
                    userId, statement.getId(), statement.getTotalDue(), statement.getMinimumDue(), statement.getStatus());
            return ResponseEntity.ok(toDTO(statement));
        } catch (Exception e) {
            log.error("Error fetching current statement for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam int userId,
            @RequestParam(defaultValue = "10") int limit) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        List<Statement> statements = statementRepo.findByUser_IdOrderByMonthNumberDesc(userId);
        return ResponseEntity.ok(statements.stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/pay")
    @Transactional
    public ResponseEntity<?> payStatement(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            // Fetch statement fresh from DB (not cached/detached)
            Statement statement = statementRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Statement not found: " + id));

            double amount = ((Number) payload.getOrDefault("amount", 0.0)).doubleValue();
            int userId = statement.getUser().getId();
            
            double minimumDue = statement.getMinimumDue();
            double totalDue = statement.getTotalDue();
            
            // DEBUG: Log payment details before validation
            log.info("Processing payment for userId={}, statementId={}, totalDue={}, minimumDue={}, amount={}, status={}",
                    userId, id, totalDue, minimumDue, amount, statement.getStatus());

            // Validate amount is positive
            if (amount <= 0) {
                log.warn("Payment rejected: amount {} is not positive", amount);
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Payment amount must be positive"));
            }
            
            // If totalDue is 0, no payment is needed
            if (totalDue <= 0) {
                log.warn("Payment rejected: totalDue is {} - no payment needed", totalDue);
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "No payment is due on this statement (Total Due: $0.00)"));
            }

            // Check turns (payments consume a turn)
            if (!gameService.consumeTurn(userId)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.NO_TURNS, 
                            "You're out of turns for this month. End the month to continue."));
            }

            Resource res = resourceRepo.findByUserId(userId);
            if (res == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.RESOURCE_NOT_FOUND, 
                            "Resource not found for user " + userId));
            }

            // Check if user has enough money
            if (res.getMoney() < amount) {
                log.warn("Payment rejected: insufficient funds (has={}, needs={})", res.getMoney(), amount);
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.INSUFFICIENT_FUNDS, "Insufficient funds"));
            }

            // Validate payment amount based on statement status
            // DEMO MODE: Allow any amount > 0 up to totalDue (no minimum requirement)
            if (statement.getStatus() == Statement.StatementStatus.PAID) {
                log.warn("Payment rejected: statement {} is already PAID", id);
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "Statement is already paid"));
            }
            
            // Payment cannot exceed totalDue
            if (amount > totalDue + 0.01) { // small epsilon for floating point
                log.warn("Payment rejected: amount {} > totalDue {}", amount, totalDue);
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.PAYMENT_TOO_HIGH, 
                            String.format("Payment cannot exceed total due: $%.2f", totalDue)));
            }
            
            // Log that we're allowing flexible payments (demo mode)
            log.info("Demo mode: Allowing payment of ${} (min suggested: ${}, total: ${})", 
                    amount, minimumDue, totalDue);
            
            log.info("Payment validation passed for statementId={}, proceeding with amount={}", id, amount);

            // Create payment transaction with positive amount (normalized model)
            Transaction paymentTx = new Transaction();
            paymentTx.setUser(statement.getUser());
            paymentTx.setMerchant("Statement Payment");
            paymentTx.setAmount(Money.round2(amount)); // Store as positive
            paymentTx.setCategory("Payment");
            paymentTx.setType(Transaction.TransactionType.PAYMENT);
            paymentTx.setTimestamp(OffsetDateTime.now());
            paymentTx.setStatement(statement);
            txRepo.save(paymentTx);

            // Deduct money using Money utility
            res.setMoney(Money.subtract(res.getMoney(), amount));
            resourceRepo.save(res);

            // Calculate remaining balance after payment
            double remainingBalance = Money.subtract(totalDue, amount);
            
            // Update statement status and totalDue
            if (remainingBalance <= 0.01) { // Account for rounding
                statement.setStatus(Statement.StatementStatus.PAID);
                statement.setTotalDue(0.0);
            } else {
                statement.setTotalDue(Money.round2(remainingBalance));
                // Status remains OPEN or OVERDUE
            }
            statementRepo.save(statement);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("amount", Money.round2(amount));
            response.put("remainingBalance", statement.getTotalDue());
            response.put("status", statement.getStatus().toString());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Payment error: " + e.getMessage()));
        }
    }

    private Map<String, Object> toDTO(Statement statement) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", statement.getId());
        dto.put("monthNumber", statement.getMonthNumber());
        dto.put("periodStart", statement.getPeriodStart().toString());
        dto.put("periodEnd", statement.getPeriodEnd().toString());
        dto.put("statementDate", statement.getStatementDate().toString());
        dto.put("dueDate", statement.getDueDate().toString());
        dto.put("totalDue", statement.getTotalDue());
        dto.put("minimumDue", statement.getMinimumDue());
        dto.put("interestRate", statement.getInterestRate());
        dto.put("fees", statement.getFees());
        dto.put("status", statement.getStatus().toString());
        return dto;
    }
}
