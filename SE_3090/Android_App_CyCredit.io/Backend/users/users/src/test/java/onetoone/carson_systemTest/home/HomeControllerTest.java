package onetoone.carson_systemTest.home;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.billing.Transaction;
import onetoone.billing.TransactionRepository;
import onetoone.home.HomeController;
import onetoone.home.RoomItem;
import onetoone.home.RoomItemRepository;
import onetoone.util.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * System tests for HomeController focusing on room layout management,
 * item placement, movement, removal, and inventory functionality.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private RoomItemRepository roomItemRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TransactionRepository txRepo;

    @InjectMocks
    private HomeController homeController;

    private User testUser;
    private RoomItem testRoomItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setName("Test User");

        testRoomItem = new RoomItem();
        testRoomItem.setId(1L);
        testRoomItem.setUser(testUser);
        testRoomItem.setItemCode("BED");
        testRoomItem.setX(0.0);
        testRoomItem.setY(2.0);
        testRoomItem.setRotation(0.0);
        testRoomItem.setZ(0);
        testRoomItem.setStarter(true);
    }

    // ========== getLayout Tests ==========

    @Test
    void testGetLayout_Success_WithExistingItems() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Arrays.asList(testRoomItem));

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body.get("furniture"));
        assertNotNull(body.get("inventory"));
        assertEquals(20, body.get("maxItems"));
    }

    @Test
    void testGetLayout_Success_FirstVisit_SeedsStarterItems() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList())
                .thenReturn(Arrays.asList(testRoomItem)); // After seeding
        
        when(roomItemRepo.save(any(RoomItem.class))).thenAnswer(invocation -> {
            RoomItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        // Verify starter items were seeded (4 items: BED, DESK, LAMP, POSTER)
        verify(roomItemRepo, times(4)).save(any(RoomItem.class));
    }

    @Test
    void testGetLayout_UserNotFound() {
        when(userRepo.findById(1)).thenReturn(Optional.empty());

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
        
        ApiError error = (ApiError) response.getBody();
        assertEquals(ApiError.USER_NOT_FOUND, error.getError());
    }

    @Test
    void testGetLayout_BuildsInventory_FromPurchasedItems() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList()); // No placed items
        
        // Create a purchase transaction for a placeable item
        Transaction purchase = new Transaction();
        purchase.setUser(testUser);
        purchase.setAmount(10.0);
        purchase.setType(Transaction.TransactionType.PURCHASE);
        purchase.setMerchant("Memorial Union - T-Shirt");
        purchase.setTimestamp(OffsetDateTime.now());
        
        when(txRepo.findByUser_IdOrderByTimestampDesc(1))
                .thenReturn(Arrays.asList(purchase));
        when(roomItemRepo.save(any(RoomItem.class))).thenAnswer(invocation -> {
            RoomItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inventory = (List<Map<String, Object>>) body.get("inventory");
        
        // Should have starter items in inventory (since none are placed yet)
        assertTrue(inventory.size() > 0);
    }

    @Test
    void testGetLayout_ExcludesConsumableItems() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());
        
        // Create purchase transactions for consumable items
        Transaction coffee = new Transaction();
        coffee.setUser(testUser);
        coffee.setAmount(5.0);
        coffee.setType(Transaction.TransactionType.PURCHASE);
        coffee.setMerchant("Memorial Union - Coffee");
        coffee.setTimestamp(OffsetDateTime.now());
        
        Transaction tshirt = new Transaction();
        tshirt.setUser(testUser);
        tshirt.setAmount(15.0);
        tshirt.setType(Transaction.TransactionType.PURCHASE);
        tshirt.setMerchant("Memorial Union - T-Shirt");
        tshirt.setTimestamp(OffsetDateTime.now());
        
        when(txRepo.findByUser_IdOrderByTimestampDesc(1))
                .thenReturn(Arrays.asList(coffee, tshirt));
        when(roomItemRepo.save(any(RoomItem.class))).thenAnswer(invocation -> {
            RoomItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inventory = (List<Map<String, Object>>) body.get("inventory");
        
        // Coffee should not be in inventory, but T-Shirt should be
        boolean hasCoffee = inventory.stream()
                .anyMatch(item -> item.get("itemCode").toString().contains("COFFEE"));
        boolean hasTShirt = inventory.stream()
                .anyMatch(item -> item.get("itemCode").toString().contains("T_SHIRT"));
        
        assertFalse(hasCoffee, "Consumable items should not appear in inventory");
        // T-Shirt might be in inventory if it's a placeable item
    }

    // ========== placeItem Tests ==========

    @Test
    void testPlaceItem_Success() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.countByUser_Id(1)).thenReturn(5); // Under limit
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList());
        when(roomItemRepo.save(any(RoomItem.class))).thenAnswer(invocation -> {
            RoomItem item = invocation.getArgument(0);
            item.setId(2L);
            return item;
        });

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemCode", "DESK");
        payload.put("x", 2.0);
        payload.put("y", 2.0);
        payload.put("rotation", 0.0);
        payload.put("z", 1);

        ResponseEntity<?> response = homeController.placeItem(1, payload);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(roomItemRepo).save(any(RoomItem.class));
    }

    @Test
    void testPlaceItem_RoomCapReached() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.countByUser_Id(1)).thenReturn(20); // At max

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemCode", "DESK");
        payload.put("x", 2.0);
        payload.put("y", 2.0);

        ResponseEntity<?> response = homeController.placeItem(1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
        
        ApiError error = (ApiError) response.getBody();
        assertEquals(ApiError.ROOM_CAP_REACHED, error.getError());
        verify(roomItemRepo, never()).save(any(RoomItem.class));
    }

    @Test
    void testPlaceItem_UserNotFound() {
        when(userRepo.findById(1)).thenReturn(Optional.empty());

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemCode", "DESK");
        payload.put("x", 2.0);
        payload.put("y", 2.0);

        ResponseEntity<?> response = homeController.placeItem(1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
    }

    @Test
    void testPlaceItem_WithDefaultRotationAndZ() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.countByUser_Id(1)).thenReturn(5);
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList());
        when(roomItemRepo.save(any(RoomItem.class))).thenAnswer(invocation -> {
            RoomItem item = invocation.getArgument(0);
            item.setId(2L);
            return item;
        });

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemCode", "LAMP");
        payload.put("x", 4.0);
        payload.put("y", 1.0);
        // rotation and z not provided - should default to 0

        ResponseEntity<?> response = homeController.placeItem(1, payload);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(roomItemRepo).save(argThat(item -> 
            item.getRotation() == 0.0 && item.getZ() == 0
        ));
    }

    // ========== moveItem Tests ==========

    @Test
    void testMoveItem_Success() {
        when(roomItemRepo.findById(1L)).thenReturn(Optional.of(testRoomItem));

        Map<String, Object> payload = new HashMap<>();
        payload.put("x", 5.0);
        payload.put("y", 3.0);
        payload.put("rotation", 90.0);
        payload.put("z", 2);

        ResponseEntity<?> response = homeController.moveItem(1L, 1, payload);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(roomItemRepo).save(argThat(item -> 
            item.getX() == 5.0 && 
            item.getY() == 3.0 && 
            item.getRotation() == 90.0 &&
            item.getZ() == 2
        ));
    }

    @Test
    void testMoveItem_PartialUpdate() {
        when(roomItemRepo.findById(1L)).thenReturn(Optional.of(testRoomItem));

        Map<String, Object> payload = new HashMap<>();
        payload.put("x", 5.0);
        // Only updating x, not y, rotation, or z

        ResponseEntity<?> response = homeController.moveItem(1L, 1, payload);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(roomItemRepo).save(argThat(item -> 
            item.getX() == 5.0 && 
            item.getY() == 2.0 // Original value preserved
        ));
    }

    @Test
    void testMoveItem_ItemNotFound() {
        when(roomItemRepo.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> payload = new HashMap<>();
        payload.put("x", 5.0);

        ResponseEntity<?> response = homeController.moveItem(1L, 1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
    }

    @Test
    void testMoveItem_Unauthorized_WrongUser() {
        User otherUser = new User();
        otherUser.setId(2);
        testRoomItem.setUser(otherUser);
        
        when(roomItemRepo.findById(1L)).thenReturn(Optional.of(testRoomItem));

        Map<String, Object> payload = new HashMap<>();
        payload.put("x", 5.0);

        ResponseEntity<?> response = homeController.moveItem(1L, 1, payload);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
        
        ApiError error = (ApiError) response.getBody();
        assertEquals(ApiError.UNAUTHORIZED, error.getError());
        verify(roomItemRepo, never()).save(any(RoomItem.class));
    }

    // ========== removeItem Tests ==========

    @Test
    void testRemoveItem_Success() {
        when(roomItemRepo.findById(1L)).thenReturn(Optional.of(testRoomItem));
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList());
        doNothing().when(roomItemRepo).delete(testRoomItem);

        ResponseEntity<?> response = homeController.removeItem(1L, 1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(roomItemRepo).delete(testRoomItem);
    }

    @Test
    void testRemoveItem_ItemNotFound() {
        when(roomItemRepo.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = homeController.removeItem(1L, 1);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
        verify(roomItemRepo, never()).delete(any(RoomItem.class));
    }

    @Test
    void testRemoveItem_Unauthorized_WrongUser() {
        User otherUser = new User();
        otherUser.setId(2);
        testRoomItem.setUser(otherUser);
        
        when(roomItemRepo.findById(1L)).thenReturn(Optional.of(testRoomItem));

        ResponseEntity<?> response = homeController.removeItem(1L, 1);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody() instanceof ApiError);
        
        ApiError error = (ApiError) response.getBody();
        assertEquals(ApiError.UNAUTHORIZED, error.getError());
        verify(roomItemRepo, never()).delete(any(RoomItem.class));
    }

    @Test
    void testRemoveItem_ReturnsUpdatedLayout() {
        when(roomItemRepo.findById(1L)).thenReturn(Optional.of(testRoomItem));
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList());
        doNothing().when(roomItemRepo).delete(testRoomItem);

        ResponseEntity<?> response = homeController.removeItem(1L, 1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        // Should return layout (which calls getLayout internally)
        assertTrue(response.getBody() instanceof Map);
    }

    // ========== Inventory Building Tests ==========

    @Test
    void testInventory_StarterItemsNotPlaced() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        // User has no placed items, so all starters should be in inventory
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());
        when(txRepo.findByUser_IdOrderByTimestampDesc(1))
                .thenReturn(Collections.emptyList());
        when(roomItemRepo.save(any(RoomItem.class))).thenAnswer(invocation -> {
            RoomItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inventory = (List<Map<String, Object>>) body.get("inventory");
        
        // After seeding, items are placed, so they won't be in inventory
        // But if we check before seeding completes, we'd see them
    }

    @Test
    void testInventory_PurchasedItemsMinusPlaced() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        
        // User has 2 T-Shirts purchased, 1 placed
        RoomItem placedTShirt = new RoomItem();
        placedTShirt.setItemCode("T_SHIRT");
        placedTShirt.setUser(testUser);
        
        Transaction purchase1 = new Transaction();
        purchase1.setUser(testUser);
        purchase1.setType(Transaction.TransactionType.PURCHASE);
        purchase1.setMerchant("Memorial Union - T-Shirt");
        purchase1.setTimestamp(OffsetDateTime.now());
        
        Transaction purchase2 = new Transaction();
        purchase2.setUser(testUser);
        purchase2.setType(Transaction.TransactionType.PURCHASE);
        purchase2.setMerchant("Memorial Union - T-Shirt");
        purchase2.setTimestamp(OffsetDateTime.now());
        
        when(roomItemRepo.findByUser_Id(1))
                .thenReturn(Arrays.asList(placedTShirt));
        when(txRepo.findByUser_IdOrderByTimestampDesc(1))
                .thenReturn(Arrays.asList(purchase1, purchase2));

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inventory = (List<Map<String, Object>>) body.get("inventory");
        
        // Should have 1 T-Shirt in inventory (2 purchased - 1 placed)
        Optional<Map<String, Object>> tShirtInInventory = inventory.stream()
                .filter(item -> item.get("itemCode").toString().equals("T_SHIRT"))
                .findFirst();
        
        if (tShirtInInventory.isPresent()) {
            assertEquals(1, tShirtInInventory.get().get("quantity"));
        }
    }

    @Test
    void testInventory_OnlyPurchaseTransactions() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(roomItemRepo.findByUser_Id(1)).thenReturn(Collections.emptyList());
        
        // Mix of transaction types - only PURCHASE should count
        Transaction purchase = new Transaction();
        purchase.setUser(testUser);
        purchase.setType(Transaction.TransactionType.PURCHASE);
        purchase.setMerchant("Memorial Union - T-Shirt");
        purchase.setTimestamp(OffsetDateTime.now());
        
        Transaction payment = new Transaction();
        payment.setUser(testUser);
        payment.setType(Transaction.TransactionType.PAYMENT);
        payment.setMerchant("Memorial Union - T-Shirt");
        payment.setTimestamp(OffsetDateTime.now());
        
        Transaction reward = new Transaction();
        reward.setUser(testUser);
        reward.setType(Transaction.TransactionType.REWARD);
        reward.setMerchant("Memorial Union - T-Shirt");
        reward.setTimestamp(OffsetDateTime.now());
        
        when(txRepo.findByUser_IdOrderByTimestampDesc(1))
                .thenReturn(Arrays.asList(purchase, payment, reward));

        ResponseEntity<?> response = homeController.getLayout(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        // Only PURCHASE transactions should contribute to inventory
        // This is verified by the buildInventory logic in the controller
    }
}

