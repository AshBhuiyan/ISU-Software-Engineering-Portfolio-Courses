package onetoone.store;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.util.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/store/memorial-union")
public class StoreController {

    private final StoreService store;

    public StoreController(StoreService store) {
        this.store = store;
    }
    @Operation(
            summary = "List all store items",
            description = "Fetches a list of all items available in the Memorial Union store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of store items returned successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/items")
    public ResponseEntity<List<StoreItem>> items() {
        return ResponseEntity.ok(store.listItems());
    }
    @Operation(
            summary = "Purchase an item",
            description = "Allows a user to purchase a specified quantity of an item. Returns the new cash balance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase successful"),
            @ApiResponse(responseCode = "400", description = "Insufficient funds or invalid request"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(@RequestBody PurchaseRequest req) {
        try {
            // Validate purchaseNonce is provided
            if (req.getPurchaseNonce() == null || req.getPurchaseNonce().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, "purchaseNonce is required"));
            }
            
            double newCash = store.purchase(req.getUserId(), req.getItemId(), req.getQty(), req.getPurchaseNonce());
            return ResponseEntity.ok(new PurchaseResponse("OK", newCash));
        } catch (IllegalStateException ise) {
            String message = ise.getMessage();
            if ("NO_TURNS".equals(message)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.NO_TURNS, "You're out of turns for this month. End the month to continue."));
            } else if ("INSUFFICIENT_FUNDS".equals(message)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.INSUFFICIENT_FUNDS, "Insufficient funds"));
            } else if ("OUT_OF_CREDIT".equals(message)) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.OUT_OF_CREDIT, "Purchase would exceed credit limit"));
            } else if (message != null && message.contains("Resource not found")) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.RESOURCE_NOT_FOUND, message));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.BAD_REQUEST, message != null ? message : "Purchase failed"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Purchase error: " + ex.getMessage()));
        }
    }
    @Operation(
            summary = "Update a store item",
            description = "Updates an existing store item by its ID with the provided item details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(@PathVariable int id, @RequestBody StoreItem updatedItem) {
        try {
            StoreItem item = store.updateItem(id, updatedItem);
            return ResponseEntity.ok(item);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        }
    }
    @Operation(
            summary = "Delete a store item",
            description = "Deletes a store item by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable int id) {
        try {
            store.deleteItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        }
    }
}
