package onetoone.billing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.game.GameService;
import onetoone.util.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billing;
    private final GameService gameService;

    public BillingController(BillingService billing, GameService gameService) {
        this.billing = billing;
        this.gameService = gameService;
    }
    @Operation(
            summary = "Get billing summary for a user",
            description = "Returns a financial summary for the given user, including totals, spending, and categorized breakdowns."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/summary/{userId}")
    public ResponseEntity<?> summary(@PathVariable int userId) {
        try {
            return ResponseEntity.ok(billing.getSummary(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Summary error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "List all transactions for a user",
            description = "Returns a list of all billing transactions associated with the provided user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions fetched successfully"),
            @ApiResponse(responseCode = "404", description = "User not found or no transactions available"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/transactions")
    public ResponseEntity<?> list(@RequestParam int userId) {
        try {
            var txs = billing.listTransactions(userId).stream()
                    .map(TransactionDTO::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(txs);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "List transactions error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Create a new transaction",
            description = "Adds a new transaction to the billing records for the given user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/transactions")
    public ResponseEntity<?> create(@RequestBody TransactionDTO dto, @RequestParam int userId) {
        try {
            var created = billing.createTransaction(userId, dto.merchant, dto.amount, dto.category);
            return ResponseEntity.ok(TransactionDTO.from(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.USER_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Create transaction error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Update an existing transaction",
            description = "Modifies the merchant, amount, or category of an existing transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/transactions/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody TransactionDTO dto) {
        try {
            var updated = billing.updateTransaction(id, dto.merchant, dto.amount, dto.category);
            return ResponseEntity.ok(TransactionDTO.from(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Update transaction error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Delete a transaction",
            description = "Deletes a transaction from the billing system using its transaction ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        try {
            billing.deleteTransaction(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Delete transaction error: " + e.getMessage()));
        }
    }

    public static class TransactionDTO {
        public String merchant;
        public double amount;
        public String timestamp;
        public String category;
        public String type; // Transaction type: PURCHASE, PAYMENT, INCOME, INTEREST, FEE, REWARD

        public static TransactionDTO from(Transaction t) {
            var dto = new TransactionDTO();
            dto.merchant = t.getMerchant();
            dto.amount = t.getAmount();
            dto.timestamp = t.getTimestamp() != null ? t.getTimestamp().toString() : "1970-01-01T00:00:00Z";
            dto.category = t.getCategory() != null ? t.getCategory() : "Other";
            dto.type = t.getType() != null ? t.getType().name() : "PURCHASE";
            return dto;
        }
    }
}
