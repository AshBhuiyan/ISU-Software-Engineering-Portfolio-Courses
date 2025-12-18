package onetoone.store;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.billing.BillingService;
import onetoone.game.GameService;
import onetoone.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * StoreService handles Memorial Union store purchases.
 * Consumes turns and uses normalized transaction model.
 */
@Service
public class StoreService {
    private final BillingService billing;
    private final StoreItemRepository repo;
    private final ResourceRepository resourceRepo;
    private final GameService gameService;

    public StoreService(BillingService billing, StoreItemRepository repo, ResourceRepository resourceRepo, GameService gameService) {
        this.billing = billing;
        this.repo = repo;
        this.resourceRepo = resourceRepo;
        this.gameService = gameService;
        seedStoreItems();
    }
    
    private void seedStoreItems() {
        // Check if we need to re-seed (old items have different count or missing categories)
        if (repo.count() > 0 && repo.count() < 50) {
            // Old seed had fewer items - clear and re-seed with new 56 items
            repo.deleteAll();
        }
        
        if (repo.count() == 0) {
            // ===== SNACKS & DRINKS (Cheap - $1 to $10) =====
            repo.save(new StoreItem("MU Coffee", "Hot coffee to fuel your studies", 2.50, "Snacks", "coffee"));
            repo.save(new StoreItem("Giant Cookie", "Fresh baked chocolate chip cookie", 1.99, "Snacks", "cookie"));
            repo.save(new StoreItem("Boba Tea", "Classic milk tea with tapioca pearls", 5.99, "Snacks", "boba"));
            repo.save(new StoreItem("Pizza Slice", "Cheese or pepperoni - your choice!", 3.99, "Snacks", "pizza"));
            repo.save(new StoreItem("Burrito Bowl", "Loaded with rice, beans, and more", 8.99, "Snacks", "burrito"));
            repo.save(new StoreItem("Energy Drink", "Cyclone Power energy drink", 3.49, "Snacks", "energy"));
            repo.save(new StoreItem("Donut Box", "Half dozen assorted donuts", 7.99, "Snacks", "donut"));
            repo.save(new StoreItem("Smoothie", "Fresh fruit smoothie blend", 6.49, "Snacks", "smoothie"));
            
            // ===== SUPPLIES (Budget - $5 to $30) =====
            repo.save(new StoreItem("Blue Book", "Standard exam booklet", 0.99, "Supplies", "bluebook"));
            repo.save(new StoreItem("Pen Pack", "Set of 5 gel pens", 4.99, "Supplies", "pens"));
            repo.save(new StoreItem("ISU Notebook", "Spiral notebook with logo", 6.99, "Supplies", "notebook"));
            repo.save(new StoreItem("Study Earbuds", "Cheap earbuds for library", 12.99, "Supplies", "earbuds"));
            repo.save(new StoreItem("Water Bottle", "Reusable ISU water bottle", 14.99, "Supplies", "bottle"));
            repo.save(new StoreItem("Basic Backpack", "Simple backpack for books", 29.99, "Supplies", "backpack"));
            
            // ===== APPAREL (Mid-range - $20 to $80) =====
            repo.save(new StoreItem("ISU Cap", "Classic cardinal baseball cap", 19.99, "Apparel", "cap"));
            repo.save(new StoreItem("ISU T-Shirt", "Cardinal red with gold logo", 24.99, "Apparel", "tshirt"));
            repo.save(new StoreItem("Cyclone Socks", "Cozy ISU themed socks", 12.99, "Apparel", "socks"));
            repo.save(new StoreItem("ISU Sweatpants", "Comfortable joggers", 44.99, "Apparel", "pants"));
            repo.save(new StoreItem("ISU Hoodie", "Warm hoodie with Cy logo", 54.99, "Apparel", "hoodie"));
            repo.save(new StoreItem("Premium Jacket", "Official windbreaker", 79.99, "Apparel", "jacket"));
            
            // ===== FUN STUFF (Mid-range - $15 to $100) =====
            repo.save(new StoreItem("Fidget Spinner", "ISU themed spinner", 9.99, "Fun", "spinner"));
            repo.save(new StoreItem("Plush Cy", "Adorable mascot plushie", 24.99, "Fun", "plush"));
            repo.save(new StoreItem("Nerf Blaster", "Foam dart blaster", 19.99, "Fun", "nerf"));
            repo.save(new StoreItem("Board Game Night", "Cards Against Humanity", 29.99, "Fun", "cards"));
            repo.save(new StoreItem("Retro Controller", "USB NES-style controller", 24.99, "Fun", "controller"));
            repo.save(new StoreItem("Ukulele", "Learn to play on campus!", 49.99, "Fun", "ukulele"));
            repo.save(new StoreItem("Polaroid Camera", "Instant photo memories", 69.99, "Fun", "polaroid"));
            repo.save(new StoreItem("Mini Skateboard", "Fingerboard for desk fun", 14.99, "Fun", "skateboard"));
            
            // ===== TECH (Premium - $30 to $200) =====
            repo.save(new StoreItem("Phone Charger", "Fast charging cable", 14.99, "Tech", "charger"));
            repo.save(new StoreItem("Phone Case", "ISU protective case", 24.99, "Tech", "phonecase"));
            repo.save(new StoreItem("USB Drive 64GB", "ISU branded flash drive", 19.99, "Tech", "usb"));
            repo.save(new StoreItem("Wireless Earbuds", "Premium sound quality", 49.99, "Tech", "airpods"));
            repo.save(new StoreItem("Gaming Mouse", "RGB gaming mouse", 39.99, "Tech", "mouse"));
            repo.save(new StoreItem("Mech Keyboard", "Cherry MX switches", 89.99, "Tech", "keyboard"));
            repo.save(new StoreItem("Switch Game", "Latest Nintendo release", 59.99, "Tech", "switch"));
            repo.save(new StoreItem("Portable Monitor", "15-inch USB-C display", 149.99, "Tech", "monitor"));
            
            // ===== LUXURY (Expensive - $100 to $500) =====
            repo.save(new StoreItem("Nike Air Max", "ISU custom colorway", 149.99, "Luxury", "shoes"));
            repo.save(new StoreItem("Smart Watch", "Fitness tracker deluxe", 199.99, "Luxury", "watch"));
            repo.save(new StoreItem("AirPods Pro", "Noise cancelling earbuds", 249.99, "Luxury", "airpodspro"));
            repo.save(new StoreItem("Designer Backpack", "Premium leather bag", 199.99, "Luxury", "luxbag"));
            repo.save(new StoreItem("Ray-Ban Sunglasses", "Classic Wayfarer style", 179.99, "Luxury", "sunglasses"));
            repo.save(new StoreItem("GoPro Camera", "Action camera for adventures", 299.99, "Luxury", "gopro"));
            
            // ===== EPIC (Very Expensive - $300+) =====
            repo.save(new StoreItem("PS5 Controller", "DualSense wireless", 69.99, "Epic", "ps5controller"));
            repo.save(new StoreItem("Gaming Monitor", "27-inch 144Hz display", 299.99, "Epic", "gamingmonitor"));
            repo.save(new StoreItem("Nintendo Switch", "Portable gaming console", 299.99, "Epic", "nintendoswitch"));
            repo.save(new StoreItem("iPad Mini", "Tablet for notes and more", 499.99, "Epic", "ipad"));
            repo.save(new StoreItem("PS5 Console", "Next-gen gaming!", 499.99, "Epic", "ps5"));
            repo.save(new StoreItem("MacBook Air", "M2 chip, 8GB RAM", 999.99, "Epic", "macbook"));
            repo.save(new StoreItem("Campus Scooter", "Electric scooter for campus", 449.99, "Epic", "scooter"));
            repo.save(new StoreItem("Gold Cy Statue", "Limited edition collectible", 999.99, "Epic", "goldcy"));
        }
    }

    /**
     * Purchase an item from the store.
     * Consumes a turn and creates a purchase transaction.
     * Idempotent: duplicate purchaseNonce returns same result.
     */
    @Transactional
    public double purchase(int userId, int itemId, int qty, String purchaseNonce) {
        // Validate purchaseNonce is provided
        if (purchaseNonce == null || purchaseNonce.isEmpty()) {
            throw new IllegalStateException("purchaseNonce is required");
        }
        
        // Check for duplicate nonce (idempotency)
        var existingTx = billing.findTransactionByPurchaseNonce(purchaseNonce);
        if (existingTx.isPresent()) {
            // Already processed - return current balance (idempotent)
            Resource res = resourceRepo.findByUserId(userId);
            return res != null ? Money.round2(res.getMoney()) : 0.0;
        }
        
        // Check turns
        if (!gameService.consumeTurn(userId)) {
            throw new IllegalStateException("NO_TURNS");
        }

        StoreItem item = repo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        
        int q = Math.max(1, qty);
        // Use Money utility for price calculation
        double total = Money.multiply(item.getPrice(), q);

        Resource r = resourceRepo.findByUserId(userId);
        if (r == null) {
            throw new IllegalStateException("Resource not found for user " + userId);
        }
        
        // Check if user has enough money or credit
        double currentBalance = billing.getCurrentBalance(userId);
        if (r.getMoney() < total && (currentBalance + total) > r.getCreditLimit()) {
            throw new IllegalStateException("OUT_OF_CREDIT");
        }
        if (r.getMoney() < total && (currentBalance + total) <= r.getCreditLimit()) {
            // Can use credit - proceed
        } else if (r.getMoney() < total) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }

        // Apply charge (uses normalized transaction model) with purchaseNonce for idempotency
        billing.applyCharge(userId, "Memorial Union - " + item.getName(), total, "MU Purchase", OffsetDateTime.now(), purchaseNonce);

        // Return updated money (rounded)
        Resource updated = resourceRepo.findByUserId(userId);
        return updated != null ? Money.round2(updated.getMoney()) : 0.0;
    }

    public List<StoreItem> listItems() {
        return repo.findAll();
    }

    public StoreItem updateItem(int id, StoreItem updatedItem) {
        StoreItem item = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        item.setName(updatedItem.getName());
        item.setPrice(Money.round2(updatedItem.getPrice())); // Round price
        return repo.save(item);
    }

    public void deleteItem(int id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Item not found: " + id);
        }
        repo.deleteById(id);
    }
}
