package onetoone.billing;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.util.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StatementService {

    private static final Logger log = LoggerFactory.getLogger(StatementService.class);
    
    // Minimum payment constants (DEMO-FRIENDLY values)
    private static final double MINIMUM_PAYMENT_FLOOR = 5.0;    // Reduced from $25 to $5
    private static final double MINIMUM_PAYMENT_PERCENT = 0.10; // 10% of total
    private static final int STATEMENT_GRACE_PERIOD_DAYS = 7;
    private static final double DEFAULT_INTEREST_RATE = 0.199; // 19.9% APR

    private final StatementRepository statementRepo;
    private final UserRepository userRepo;
    private final TransactionRepository txRepo;
    private final BillingService billingService;

    public StatementService(StatementRepository statementRepo, UserRepository userRepo,
                           TransactionRepository txRepo, BillingService billingService) {
        this.statementRepo = statementRepo;
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.billingService = billingService;
    }

    /**
     * Get the current payable statement for a user.
     * Prioritizes OVERDUE statements (must pay first), then OPEN statements with balance > 0.
     * If no statement exists but user has an outstanding balance, generates one on-demand.
     * 
     * KEY FIX: If an OPEN statement has totalDue <= 0, it's fully paid - mark it PAID
     * and check for new unbilled activity.
     */
    @Transactional
    public Statement getCurrentStatement(int userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // First check for OVERDUE statements (they take priority)
        Optional<Statement> overdue = statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                userId, Statement.StatementStatus.OVERDUE);
        if (overdue.isPresent()) {
            Statement stmt = overdue.get();
            // If OVERDUE but totalDue is 0, mark as PAID and continue
            if (stmt.getTotalDue() <= 0.01) {
                log.info("OVERDUE statement {} has totalDue=0, marking as PAID", stmt.getId());
                stmt.setStatus(Statement.StatementStatus.PAID);
                statementRepo.save(stmt);
            } else {
                log.info("Found OVERDUE statement for userId={}: id={}, totalDue={}", 
                        userId, stmt.getId(), stmt.getTotalDue());
                return stmt;
            }
        }
        
        // Then check for OPEN statements
        Optional<Statement> open = statementRepo.findFirstByUser_IdAndStatusOrderByMonthNumberDesc(
                userId, Statement.StatementStatus.OPEN);
        if (open.isPresent()) {
            Statement stmt = open.get();
            // KEY FIX: If OPEN statement has totalDue <= 0, it's fully paid
            // Mark it as PAID and fall through to check for new unbilled balance
            if (stmt.getTotalDue() <= 0.01) {
                log.info("OPEN statement {} has totalDue={}, marking as PAID and checking for new activity",
                        stmt.getId(), stmt.getTotalDue());
                stmt.setStatus(Statement.StatementStatus.PAID);
                statementRepo.save(stmt);
                // Don't return - fall through to check for unbilled balance
            } else {
                log.info("Found OPEN statement for userId={}: id={}, totalDue={}", 
                        userId, stmt.getId(), stmt.getTotalDue());
                return stmt;
            }
        }
        
        // No active statement with balance > 0 - check if user has an unbilled balance
        log.info("No active statement for userId={}, checking for unbilled balance...", userId);
        double unbilledBalance = billingService.getUnbilledBalance(userId);
        
        if (unbilledBalance > 0.01) { // Small epsilon for floating point comparison
            log.info("User {} has unbilled balance of ${}, generating on-demand statement", 
                    userId, Money.round2(unbilledBalance));
            return generateOnDemandStatement(user, unbilledBalance);
        }
        
        log.info("No payable statement and no outstanding balance for userId={}", userId);
        return null;
    }
    
    /**
     * Generate an on-demand statement for a user with outstanding balance.
     */
    private Statement generateOnDemandStatement(User user, double totalDue) {
        LocalDate today = LocalDate.now();
        
        // Calculate minimum due: max(floor, percentage), but never exceed totalDue
        double computedMin = Math.max(MINIMUM_PAYMENT_FLOOR, totalDue * MINIMUM_PAYMENT_PERCENT);
        double minimumDue = Math.min(totalDue, computedMin);
        
        // Determine month number (find max existing month + 1, or start at 1)
        List<Statement> existingStatements = statementRepo.findByUser_IdOrderByMonthNumberDesc(user.getId());
        int monthNumber = existingStatements.isEmpty() ? 1 : existingStatements.get(0).getMonthNumber() + 1;
        
        Statement statement = new Statement();
        statement.setUser(user);
        statement.setMonthNumber(monthNumber);
        statement.setPeriodStart(today.withDayOfMonth(1));
        statement.setPeriodEnd(today);
        statement.setStatementDate(today);
        statement.setDueDate(today.plusDays(STATEMENT_GRACE_PERIOD_DAYS));
        statement.setTotalDue(Money.round2(totalDue));
        statement.setMinimumDue(Money.round2(minimumDue));
        statement.setInterestRate(DEFAULT_INTEREST_RATE);
        statement.setFees(0.0);
        statement.setStatus(Statement.StatementStatus.OPEN);
        
        Statement saved = statementRepo.save(statement);
        
        // Link unbilled transactions to this statement
        linkUnbilledTransactions(user.getId(), saved);
        
        log.info("Generated on-demand statement for userId={}: id={}, totalDue=${}, minimumDue=${}, dueDate={}",
                user.getId(), saved.getId(), saved.getTotalDue(), saved.getMinimumDue(), saved.getDueDate());
        
        return saved;
    }
    
    /**
     * Link all unbilled transactions to the given statement.
     */
    private void linkUnbilledTransactions(int userId, Statement statement) {
        List<Transaction> transactions = txRepo.findByUser_IdOrderByTimestampDesc(userId);
        int linked = 0;
        
        for (Transaction tx : transactions) {
            if (tx.getStatement() == null) {
                tx.setStatement(statement);
                txRepo.save(tx);
                linked++;
            }
        }
        
        log.debug("Linked {} unbilled transactions to statement {} for userId={}", 
                linked, statement.getId(), userId);
    }
}

