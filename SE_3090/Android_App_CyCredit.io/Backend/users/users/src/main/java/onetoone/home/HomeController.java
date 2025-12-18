package onetoone.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.util.ApiError;
import onetoone.home.RoomItem;
import onetoone.home.RoomItemRepository;
import onetoone.billing.Transaction;
import onetoone.billing.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/home")
public class HomeController {

    private static final int MAX_ITEMS = 20;
    private static final String[] STARTER_ITEMS = {"BED", "DESK", "LAMP", "POSTER"};
    
    // Categories of MU items that can be placed in room (non-food/consumable items)
    private static final Set<String> PLACEABLE_CATEGORIES = Set.of(
        "Supplies", "Apparel", "Fun", "Tech", "Luxury", "Epic", "Memorabilia", "Technology"
    );

    private final RoomItemRepository roomItemRepo;
    private final UserRepository userRepo;
    private final TransactionRepository txRepo;

    public HomeController(RoomItemRepository roomItemRepo, UserRepository userRepo, TransactionRepository txRepo) {
        this.roomItemRepo = roomItemRepo;
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        seedStarterItems();
    }

    private void seedStarterItems() {
        // This will be called per-user on first visit
    }
    @Operation(
            summary = "Get layout for a user",
            description = "Returns the full layout of the user's home including all items placed inside."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layout retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/layout")
    public ResponseEntity<?> getLayout(@RequestParam int userId) {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Seed starters only on first visit (when user has no items at all)
            List<RoomItem> existingItems = roomItemRepo.findByUser_Id(userId);
            if (existingItems.isEmpty()) {
                seedStarterItems(user);
                existingItems = roomItemRepo.findByUser_Id(userId); // Refresh after seeding
            }

            List<Map<String, Object>> furniture = new ArrayList<>();
            for (RoomItem item : existingItems) {
                furniture.add(toItemDTO(item));
            }

            // Build inventory (starters that are not placed go to inventory)
            Map<String, Integer> inventory = buildInventory(userId, existingItems);

            Map<String, Object> response = new HashMap<>();
            response.put("furniture", furniture);
            response.put("inventory", inventoryToList(inventory));
            response.put("maxItems", MAX_ITEMS);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.USER_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Layout error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Place a new item",
            description = "Adds a new furniture item to the user’s home layout."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid item placement data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/place")
    @Transactional
    public ResponseEntity<?> placeItem(
            @RequestParam int userId,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            int currentCount = roomItemRepo.countByUser_Id(userId);
            if (currentCount >= MAX_ITEMS) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.ROOM_CAP_REACHED, 
                            "Room cap reached (20 items). Remove items to place more."));
            }

            String itemCode = (String) payload.get("itemCode");
            double x = ((Number) payload.get("x")).doubleValue();
            double y = ((Number) payload.get("y")).doubleValue();
            double rotation = payload.containsKey("rotation") ? 
                    ((Number) payload.get("rotation")).doubleValue() : 0.0;
            int z = payload.containsKey("z") ? 
                    ((Number) payload.get("z")).intValue() : 0;

            // Check if item is in inventory (not already placed)
            List<RoomItem> existing = roomItemRepo.findByUser_Id(userId);
            boolean inInventory = false;
            for (String starter : STARTER_ITEMS) {
                if (starter.equals(itemCode)) {
                    long count = existing.stream()
                            .filter(i -> i.getItemCode().equals(itemCode) && !i.isStarter())
                            .count();
                    inInventory = count == 0; // Starter not placed yet
                    break;
                }
            }
            // For non-starter items, assume they're in inventory if not placed

            RoomItem item = new RoomItem();
            item.setUser(user);
            item.setItemCode(itemCode);
            item.setX(x);
            item.setY(y);
            item.setRotation(rotation);
            item.setZ(z);
            item.setStarter(Arrays.asList(STARTER_ITEMS).contains(itemCode));
            roomItemRepo.save(item);

            return ResponseEntity.ok(toItemDTO(item));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Place error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Move an existing item",
            description = "Updates the position or orientation of an existing furniture item."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item moved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid movement data"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PatchMapping("/move/{id}")
    @Transactional
    public ResponseEntity<?> moveItem(
            @PathVariable Long id,
            @RequestParam int userId,
            @RequestBody Map<String, Object> payload) {
        try {
            RoomItem item = roomItemRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + id));

            if (item.getUser().getId() != userId) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.UNAUTHORIZED, "Item does not belong to user"));
            }

            if (payload.containsKey("x")) {
                item.setX(((Number) payload.get("x")).doubleValue());
            }
            if (payload.containsKey("y")) {
                item.setY(((Number) payload.get("y")).doubleValue());
            }
            if (payload.containsKey("rotation")) {
                item.setRotation(((Number) payload.get("rotation")).doubleValue());
            }
            if (payload.containsKey("z")) {
                item.setZ(((Number) payload.get("z")).intValue());
            }

            roomItemRepo.save(item);
            return ResponseEntity.ok(toItemDTO(item));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Move error: " + e.getMessage()));
        }
    }
    @Operation(
            summary = "Remove a furniture item",
            description = "Deletes a furniture item from the user's home layout and returns the updated layout."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid removal request"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/remove/{id}")
    @Transactional
    public ResponseEntity<?> removeItem(
            @PathVariable Long id,
            @RequestParam int userId) {
        try {
            RoomItem item = roomItemRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + id));

            if (item.getUser().getId() != userId) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(ApiError.UNAUTHORIZED, "Item does not belong to user"));
            }

            boolean wasStarter = item.isStarter();
            roomItemRepo.delete(item);

            // If it was a starter, it goes to inventory (not auto-replaced)
            // Return updated layout which will show it in inventory
            return getLayout(userId);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(ApiError.BAD_REQUEST, "Remove error: " + e.getMessage()));
        }
    }

    private void seedStarterItems(User user) {
        // Only seed on first visit - place starters at default grid positions (0-7 x, 0-5 y)
        // BED in corner, DESK nearby, LAMP on desk area, POSTER on wall
        int[][] positions = {{0, 2}, {2, 2}, {4, 1}, {6, 0}};
        for (int i = 0; i < STARTER_ITEMS.length; i++) {
            RoomItem starter = new RoomItem();
            starter.setUser(user);
            starter.setItemCode(STARTER_ITEMS[i]);
            starter.setX(positions[i % positions.length][0]);
            starter.setY(positions[i % positions.length][1]);
            starter.setRotation(0);
            starter.setZ(i); // Stack order
            starter.setStarter(true);
            roomItemRepo.save(starter);
        }
    }

    private Map<String, Integer> buildInventory(int userId, List<RoomItem> placedItems) {
        Map<String, Integer> inventory = new HashMap<>();
        
        // Starters that are not placed go to inventory
        for (String starter : STARTER_ITEMS) {
            boolean isPlaced = placedItems.stream()
                    .anyMatch(i -> i.getItemCode().equals(starter));
            if (!isPlaced) {
                inventory.put(starter, 1);
            }
        }

        // Add purchased MU items (non-food/consumable) to inventory
        List<Transaction> purchases = txRepo.findByUser_IdOrderByTimestampDesc(userId);
        Map<String, Integer> purchasedItems = new HashMap<>();
        
        for (Transaction tx : purchases) {
            if (tx.getType() != Transaction.TransactionType.PURCHASE) continue;
            
            String merchant = tx.getMerchant();
            if (merchant == null || !merchant.startsWith("Memorial Union - ")) continue;
            
            // Extract item name from "Memorial Union - Item Name"
            String itemName = merchant.substring("Memorial Union - ".length()).trim();
            
            // Skip food/consumable items (Snacks category items)
            if (isConsumableItem(itemName)) continue;
            
            // Convert to item code (uppercase, spaces to underscores)
            String itemCode = itemName.toUpperCase().replace(" ", "_");
            purchasedItems.merge(itemCode, 1, Integer::sum);
        }
        
        // Subtract items that are already placed in the room
        for (Map.Entry<String, Integer> entry : purchasedItems.entrySet()) {
            String itemCode = entry.getKey();
            int totalPurchased = entry.getValue();
            
            // Count how many of this item are already placed
            long placedCount = placedItems.stream()
                    .filter(i -> i.getItemCode().equalsIgnoreCase(itemCode))
                    .count();
            
            int available = totalPurchased - (int) placedCount;
            if (available > 0) {
                inventory.put(itemCode, available);
            }
        }

        return inventory;
    }
    
    /**
     * Check if an item is consumable (food/drink) and shouldn't be placeable
     */
    private boolean isConsumableItem(String itemName) {
        if (itemName == null) return true;
        String lower = itemName.toLowerCase();
        // Food/drink items from the Snacks category
        return lower.contains("coffee") || 
               lower.contains("cookie") || 
               lower.contains("boba") ||
               lower.contains("pizza") ||
               lower.contains("burrito") ||
               lower.contains("energy drink") ||
               lower.contains("donut") ||
               lower.contains("smoothie") ||
               lower.contains("sandwich") ||
               lower.contains("tea") ||
               lower.contains("food");
    }

    private List<Map<String, Object>> inventoryToList(Map<String, Integer> inventory) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("itemCode", entry.getKey());
            item.put("quantity", entry.getValue());
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> toItemDTO(RoomItem item) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", item.getId());
        dto.put("itemCode", item.getItemCode());
        dto.put("x", item.getX());
        dto.put("y", item.getY());
        dto.put("rotation", item.getRotation());
        dto.put("z", item.getZ());
        dto.put("isStarter", item.isStarter());
        return dto;
    }
}
