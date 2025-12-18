package onetoone.chase_systemTest.billing;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.billing.BillingService;
import onetoone.billing.Transaction;
import onetoone.billing.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for BillingService with normalized transaction model (all positive amounts).
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private TransactionRepository txRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ResourceRepository resourceRepo;

    @InjectMocks
    private BillingService billingService;

    private User testUser;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setName("Test User");

        testResource = new Resource();
        testResource.setId(1);
        testResource.setUser(testUser);
        testResource.setMoney(1000.0);
        testResource.setCreditLimit(1500.0);
    }

    @Test
    void testGetSummary_WithMixedTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Purchase (positive amount, increases balance)
        Transaction purchase = new Transaction();
        purchase.setType(Transaction.TransactionType.PURCHASE);
        purchase.setAmount(100.0); // Positive
        purchase.setTimestamp(OffsetDateTime.now());
        transactions.add(purchase);
        
        // Payment (positive amount, decreases balance)
        Transaction payment = new Transaction();
        payment.setType(Transaction.TransactionType.PAYMENT);
        payment.setAmount(50.0); // Positive
        payment.setTimestamp(OffsetDateTime.now());
        transactions.add(payment);
        
        // Income (positive amount, decreases balance)
        Transaction income = new Transaction();
        income.setType(Transaction.TransactionType.INCOME);
        income.setAmount(25.0); // Positive
        income.setTimestamp(OffsetDateTime.now());
        transactions.add(income);

        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(transactions);
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);

        BillingService.SummaryDTO summary = billingService.getSummary(1);

        // Balance = 100 (purchase) - 50 (payment) - 25 (income) = 25
        assertEquals(25.0, summary.balance, 0.01);
        assertEquals(100.0, summary.monthlySpend, 0.01);
        assertEquals(1500.0, summary.creditLimit, 0.01);
    }

    @Test
    void testApplyCharge_SufficientFunds() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingService.applyCharge(1, "Test Store", 50.0, "Purchase", OffsetDateTime.now());

        // Money should be deducted
        assertEquals(950.0, testResource.getMoney(), 0.01);
        verify(resourceRepo).save(testResource);
        verify(txRepo).save(any(Transaction.class));
    }

    @Test
    void testApplyCharge_InsufficientFundsButWithinCredit() {
        testResource.setMoney(10.0);
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);
        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(new ArrayList<>());
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Should succeed (uses credit)
        billingService.applyCharge(1, "Test Store", 50.0, "Purchase", OffsetDateTime.now());

        verify(txRepo).save(any(Transaction.class));
    }

    @Test
    void testApplyPayment_Success() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingService.applyPayment(1, 100.0, OffsetDateTime.now());

        // Money should be deducted
        assertEquals(900.0, testResource.getMoney(), 0.01);
        verify(resourceRepo).save(testResource);
        verify(txRepo).save(any(Transaction.class));
    }

    @Test
    void testApplyPayment_InsufficientFunds() {
        testResource.setMoney(50.0);
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(resourceRepo.findByUserId(1)).thenReturn(testResource);

        assertThrows(IllegalStateException.class, () -> {
            billingService.applyPayment(1, 100.0, OffsetDateTime.now());
        });
    }

    @Test
    void testCreateTransaction_AlwaysPositive() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = billingService.createTransaction(1, "Test", -50.0, "Category", 
                Transaction.TransactionType.PURCHASE);

        // Amount should be stored as positive (normalized)
        assertEquals(50.0, tx.getAmount(), 0.01);
        assertEquals(Transaction.TransactionType.PURCHASE, tx.getType());
    }

    @Test
    void testGetCurrentBalance_NormalizedModel() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction purchase = new Transaction();
        purchase.setType(Transaction.TransactionType.PURCHASE);
        purchase.setAmount(200.0); // Positive
        transactions.add(purchase);
        
        Transaction payment = new Transaction();
        payment.setType(Transaction.TransactionType.PAYMENT);
        payment.setAmount(75.0); // Positive
        transactions.add(payment);
        
        Transaction interest = new Transaction();
        interest.setType(Transaction.TransactionType.INTEREST);
        interest.setAmount(10.0); // Positive
        transactions.add(interest);

        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(transactions);

        double balance = billingService.getCurrentBalance(1);

        // Balance = 200 (purchase) + 10 (interest) - 75 (payment) = 135
        assertEquals(135.0, balance, 0.01);
    }
}

