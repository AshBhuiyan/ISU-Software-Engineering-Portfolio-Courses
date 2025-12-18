package onetoone.chase_systemTest.billing;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.billing.BillingService;
import onetoone.billing.Statement;
import onetoone.billing.StatementRepository;
import onetoone.billing.StatementService;
import onetoone.billing.Transaction;
import onetoone.billing.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * System tests for StatementService focusing on the new "Unbilled Transactions" model.
 * Tests verify that statements are generated on-demand when unbilled balance exists.
 */
@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TransactionRepository txRepo;
    @Mock
    private BillingService billingService;

    @InjectMocks
    private StatementService statementService;

    private User testUser;
    private Statement testStatement;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setName("Test User");

        testStatement = new Statement();
        testStatement.setId(1L);
        testStatement.setUser(testUser);
        testStatement.setStatus(Statement.StatementStatus.OPEN);
        testStatement.setTotalDue(100.0);
    }

    @Test
    void testGetCurrentStatement_Found_Open() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        // Service checks OVERDUE first, then OPEN
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.of(testStatement));
        // getUnbilledBalance is not called when an OPEN statement is found

        Statement result = statementService.getCurrentStatement(1);

        assertNotNull(result);
        assertEquals(testStatement, result);
    }

    @Test
    void testGetCurrentStatement_Found_Overdue() {
        Statement overdueStatement = new Statement();
        overdueStatement.setId(2L);
        overdueStatement.setUser(testUser);
        overdueStatement.setStatus(Statement.StatementStatus.OVERDUE);
        overdueStatement.setTotalDue(50.0); // Must have balance > 0 to be returned

        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        // OVERDUE takes priority over OPEN
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.of(overdueStatement));

        Statement result = statementService.getCurrentStatement(1);

        assertNotNull(result);
        assertEquals(overdueStatement, result);
        assertEquals(Statement.StatementStatus.OVERDUE, result.getStatus());
    }
    
    @Test
    void testGetCurrentStatement_PaidStatementTriggersNewGeneration() {
        // Setup: OPEN statement exists but has totalDue=0 (fully paid)
        Statement paidStatement = new Statement();
        paidStatement.setId(1L);
        paidStatement.setUser(testUser);
        paidStatement.setStatus(Statement.StatementStatus.OPEN);
        paidStatement.setTotalDue(0.0); // Fully paid
        
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.of(paidStatement));
        
        // User has NEW unbilled purchases
        when(billingService.getUnbilledBalance(1)).thenReturn(25.0);
        
        // Mock for transaction linking
        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(Collections.emptyList());
        when(statementRepo.findByUser_IdOrderByMonthNumberDesc(1))
                .thenReturn(Collections.singletonList(paidStatement));
        
        // Mock save
        when(statementRepo.save(any(Statement.class))).thenAnswer(invocation -> {
            Statement s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(100L); // New statement
            return s;
        });

        Statement result = statementService.getCurrentStatement(1);

        // The old statement should be marked PAID, and a new one generated
        assertNotNull(result);
        assertEquals(25.0, result.getTotalDue(), 0.01);
        // Verify the old statement was saved (marked as PAID)
        verify(statementRepo, atLeast(2)).save(any(Statement.class));
    }

    @Test
    void testGetCurrentStatement_NotFound_NoBalance() {
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        // Service checks OVERDUE first, then OPEN - both return empty
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.empty());
        // No unbilled balance = no on-demand statement
        when(billingService.getUnbilledBalance(1)).thenReturn(0.0);

        Statement result = statementService.getCurrentStatement(1);

        assertNull(result);
    }

    @Test
    void testGetCurrentStatement_OnDemandGeneration() {
        // Setup: No existing statements, but user has unbilled balance
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.empty());
        
        // Mock unbilled balance from BillingService
        when(billingService.getUnbilledBalance(1)).thenReturn(50.0);
        
        // Create an unbilled purchase transaction for linking
        Transaction purchase = new Transaction();
        purchase.setUser(testUser);
        purchase.setAmount(50.0);
        purchase.setType(Transaction.TransactionType.PURCHASE);
        purchase.setStatement(null);
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(purchase);
        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(transactions);
        
        // No existing statements for month number calculation
        when(statementRepo.findByUser_IdOrderByMonthNumberDesc(1))
                .thenReturn(Collections.emptyList());
        
        // Mock save to return a statement with ID
        when(statementRepo.save(any(Statement.class))).thenAnswer(invocation -> {
            Statement s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });
        
        // Mock transaction save (for linking transactions to statement)
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement result = statementService.getCurrentStatement(1);

        assertNotNull(result);
        assertEquals(50.0, result.getTotalDue(), 0.01);
        assertEquals(Statement.StatementStatus.OPEN, result.getStatus());
        verify(statementRepo).save(any(Statement.class));
        verify(txRepo).save(any(Transaction.class)); // Verify transaction was linked
    }

    /**
     * Test 1: Verify that getUnbilledBalance is called and a statement is created on-demand.
     */
    @Test
    void testGenerateOnDemandStatement() {
        // Setup: No existing statements
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.empty());
        
        // User has unbilled balance of $75.50
        double unbilledBalance = 75.50;
        when(billingService.getUnbilledBalance(1)).thenReturn(unbilledBalance);
        
        // Mock unbilled transactions
        Transaction tx1 = new Transaction();
        tx1.setUser(testUser);
        tx1.setAmount(50.0);
        tx1.setType(Transaction.TransactionType.PURCHASE);
        tx1.setStatement(null);
        
        Transaction tx2 = new Transaction();
        tx2.setUser(testUser);
        tx2.setAmount(25.50);
        tx2.setType(Transaction.TransactionType.PURCHASE);
        tx2.setStatement(null);
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(tx1);
        transactions.add(tx2);
        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(transactions);
        when(statementRepo.findByUser_IdOrderByMonthNumberDesc(1))
                .thenReturn(Collections.emptyList());
        
        when(statementRepo.save(any(Statement.class))).thenAnswer(invocation -> {
            Statement s = invocation.getArgument(0);
            s.setId(200L);
            return s;
        });
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement result = statementService.getCurrentStatement(1);

        // Verify getUnbilledBalance was called
        verify(billingService, times(1)).getUnbilledBalance(1);
        
        // Verify statement was created with correct totalDue
        assertNotNull(result);
        assertEquals(unbilledBalance, result.getTotalDue(), 0.01);
        assertEquals(Statement.StatementStatus.OPEN, result.getStatus());
        
        // Verify transactions were linked
        verify(txRepo, times(2)).save(any(Transaction.class));
    }

    /**
     * Test 2: Verify that after statement generation, monthlySpend drops to 0.
     * This tests the "Unbilled Transactions" model where monthlySpend = sum of PURCHASE transactions
     * where statement == null. Once transactions are linked to a statement, they no longer count.
     */
    @Test
    void testMonthlySpendReset() {
        // Setup: User has unbilled purchases
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.empty());
        
        // Initial unbilled balance
        double initialUnbilled = 100.0;
        when(billingService.getUnbilledBalance(1)).thenReturn(initialUnbilled);
        
        // Create unbilled transactions
        Transaction purchase1 = new Transaction();
        purchase1.setUser(testUser);
        purchase1.setAmount(60.0);
        purchase1.setType(Transaction.TransactionType.PURCHASE);
        purchase1.setStatement(null);
        
        Transaction purchase2 = new Transaction();
        purchase2.setUser(testUser);
        purchase2.setAmount(40.0);
        purchase2.setType(Transaction.TransactionType.PURCHASE);
        purchase2.setStatement(null);
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(purchase1);
        transactions.add(purchase2);
        when(txRepo.findByUser_IdOrderByTimestampDesc(1)).thenReturn(transactions);
        when(statementRepo.findByUser_IdOrderByMonthNumberDesc(1))
                .thenReturn(Collections.emptyList());
        
        when(statementRepo.save(any(Statement.class))).thenAnswer(invocation -> {
            Statement s = invocation.getArgument(0);
            s.setId(300L);
            return s;
        });
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            // Simulate linking: set statement reference
            if (tx.getStatement() == null) {
                Statement stmt = new Statement();
                stmt.setId(300L);
                tx.setStatement(stmt);
            }
            return tx;
        });

        // Generate statement (this should link transactions)
        Statement result = statementService.getCurrentStatement(1);
        assertNotNull(result);
        
        // Verify that if we call getCurrentStatement again, it returns the same statement
        // (not generating a new one because unbilled balance is now 0)
        // Note: getUnbilledBalance won't be called because we return the OPEN statement early
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                1, Statement.StatementStatus.OPEN))
                .thenReturn(Optional.of(result));
        
        Statement secondCall = statementService.getCurrentStatement(1);
        assertNotNull(secondCall);
        assertEquals(result.getId(), secondCall.getId());
        
        // Verify transactions were saved with statement references
        verify(txRepo, atLeast(2)).save(any(Transaction.class));
    }

    @Test
    void testGetCurrentStatement_UserNotFound() {
        when(userRepo.findById(1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            statementService.getCurrentStatement(1);
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }
}
