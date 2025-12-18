package onetoone.game;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.billing.*;
import onetoone.config.GameConfig;
import onetoone.util.Money;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameService handles game state, turn consumption, month-end processing, and credit score calculation.
 * Uses normalized transaction model: all amounts are positive.
 * Balance = sum(PURCHASE + FEE + INTEREST) - sum(PAYMENT + INCOME + REWARD)
 */
@Service
public class GameService {

    private final ResourceRepository resourceRepo;
    private final UserRepository userRepo;
    private final TransactionRepository txRepo;
    private final StatementRepository statementRepo;
    private final BillingService billingService;
    private final GameConfig config;

    public GameService(ResourceRepository resourceRepo, UserRepository userRepo,
                      TransactionRepository txRepo, StatementRepository statementRepo,
                      BillingService billingService, GameConfig config) {
        this.resourceRepo = resourceRepo;
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.statementRepo = statementRepo;
        this.billingService = billingService;
        this.config = config;
    }

    /**
     * Check if user has turns available and consume one if yes.
     * Uses optimistic locking to prevent concurrent consumption from driving turns below zero.
     * @return true if turn was consumed, false if no turns left
     * @throws IllegalStateException if resource not found or optimistic lock conflict
     */
    @Transactional
    public boolean consumeTurn(int userId) {
        try {
            Resource res = resourceRepo.findByUserId(userId);
            if (res == null) {
                throw new IllegalStateException("Resource not found for user " + userId);
            }
            if (res.getTurnsLeft() <= 0) {
                return false;
            }
            res.setTurnsLeft(res.getTurnsLeft() - 1);
            resourceRepo.save(res);
            return true;
        } catch (ObjectOptimisticLockingFailureException e) {
            // Retry once on optimistic lock failure
            Resource res = resourceRepo.findByUserId(userId);
            if (res == null) {
                throw new IllegalStateException("Resource not found for user " + userId);
            }
            if (res.getTurnsLeft() <= 0) {
                return false;
            }
            res.setTurnsLeft(res.getTurnsLeft() - 1);
            resourceRepo.save(res);
            return true;
        }
    }

    /**
     * Get current game state (turns, money, credit score)
     */
    public Resource getGameState(int userId) {
        Resource res = resourceRepo.findByUserId(userId);
        if (res == null) {
            throw new IllegalStateException("Resource not found for user " + userId);
        }
        return res;
    }

    /**
     * End the current month: generate statement, apply interest/fees, recalculate credit score
     */
    @Transactional
    public MonthlySummaryDTO endMonth(int userId) {
        Resource res = resourceRepo.findByUserId(userId);
        if (res == null) {
            throw new IllegalStateException("Resource not found for user " + userId);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        int currentMonth = res.getCurrentMonth();
        double oldCreditScore = res.getCredit();

        // Check for previous unpaid statement and mark overdue if needed
        checkAndMarkOverdue(user, res);

        // Generate statement for the current month
        Statement statement = generateStatement(user, currentMonth, res);

        // Apply interest and fees
        applyInterestAndFees(user, statement, res);

        // Recalculate credit score with breakdown
        CreditScoreBreakdown breakdown = calculateCreditScore(user, res, statement);
        res.setCredit(breakdown.finalScore);

        // Move to next month
        res.setCurrentMonth(currentMonth + 1);
        res.setTurnsLeft(config.getMaxTurnsPerMonth());
        resourceRepo.save(res);

        // Build summary
        return buildMonthlySummary(user, currentMonth, oldCreditScore, breakdown, statement);
    }

    private void checkAndMarkOverdue(User user, Resource res) {
        // Check previous statement (currentMonth - 1)
        int previousMonth = res.getCurrentMonth() - 1;
        if (previousMonth > 0) {
            statementRepo.findByUser_IdAndMonthNumber(user.getId(), previousMonth)
                    .ifPresent(prevStatement -> {
                        if (prevStatement.getStatus() == Statement.StatementStatus.OPEN) {
                            LocalDate now = LocalDate.now();
                            if (now.isAfter(prevStatement.getDueDate())) {
                                prevStatement.setStatus(Statement.StatementStatus.OVERDUE);
                                statementRepo.save(prevStatement);
                            }
                        }
                    });
        }
    }

    private Statement generateStatement(User user, int monthNumber, Resource res) {
        // Check if statement already exists
        Statement existing = statementRepo.findByUser_IdAndMonthNumber(user.getId(), monthNumber).orElse(null);
        if (existing != null) {
            return existing;
        }

        // Calculate period dates based on current month
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        LocalDate statementDate = periodEnd.plusDays(1);
        LocalDate dueDate = statementDate.plusDays(config.getStatementGracePeriodDays());

        // Get all transactions in this period using normalized model
        List<Transaction> transactions = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
        double totalDue = 0.0;
        for (Transaction tx : transactions) {
            if (tx.getTimestamp() != null) {
                LocalDate txDate = tx.getTimestamp().toLocalDate();
                if (!txDate.isBefore(periodStart) && !txDate.isAfter(periodEnd)) {
                    // All amounts are positive, so add purchases/fees/interest
                    if (tx.getType() == Transaction.TransactionType.PURCHASE ||
                        tx.getType() == Transaction.TransactionType.INTEREST ||
                        tx.getType() == Transaction.TransactionType.FEE) {
                        totalDue = Money.add(totalDue, tx.getAmount());
                    }
                    // Subtract payments (they reduce balance)
                    else if (tx.getType() == Transaction.TransactionType.PAYMENT) {
                        totalDue = Money.subtract(totalDue, tx.getAmount());
                    }
                }
            }
        }
        totalDue = Math.max(0, totalDue); // Balance can't be negative

        // Calculate minimum due: max of floor or percentage, but NEVER exceed totalDue
        // This ensures: if totalDue=$10, minimumDue=$10 (not $25 floor)
        double minimumDue;
        if (totalDue <= 0) {
            minimumDue = 0.0;
        } else {
            double computedMin = Math.max(
                    config.getMinimumPaymentFloor(),
                    Money.multiply(totalDue, config.getMinimumPaymentPercent())
            );
            minimumDue = Math.min(totalDue, computedMin);
        }
        
        // Log statement creation details
        System.out.println("[GameService] Creating statement for userId=" + user.getId() + 
            ", monthNumber=" + monthNumber +
            ", totalDue=$" + Money.round2(totalDue) + 
            ", minimumDue=$" + Money.round2(minimumDue) +
            " (floor=$" + config.getMinimumPaymentFloor() + 
            ", percent=" + (config.getMinimumPaymentPercent() * 100) + "%)");

        Statement statement = new Statement();
        statement.setUser(user);
        statement.setMonthNumber(monthNumber);
        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);
        statement.setStatementDate(statementDate);
        statement.setDueDate(dueDate);
        statement.setTotalDue(Money.round2(totalDue));
        statement.setMinimumDue(Money.round2(minimumDue));
        statement.setInterestRate(config.getInterestRateApr());
        statement.setFees(0.0);
        statement.setStatus(Statement.StatementStatus.OPEN);

        // Link transactions to statement
        for (Transaction tx : transactions) {
            if (tx.getTimestamp() != null) {
                LocalDate txDate = tx.getTimestamp().toLocalDate();
                if (!txDate.isBefore(periodStart) && !txDate.isAfter(periodEnd)) {
                    tx.setStatement(statement);
                    txRepo.save(tx);
                }
            }
        }

        return statementRepo.save(statement);
    }

    private void applyInterestAndFees(User user, Statement statement, Resource res) {
        if (statement.getStatus() == Statement.StatementStatus.PAID) {
            return; // Already paid, no interest/fees
        }

        LocalDate now = LocalDate.now();
        boolean isOverdue = now.isAfter(statement.getDueDate());

        // Calculate unpaid balance using normalized model
        double unpaidBalance = statement.getTotalDue();
        List<Transaction> payments = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
        for (Transaction tx : payments) {
            if (tx.getStatement() != null && tx.getStatement().getId().equals(statement.getId()) &&
                tx.getType() == Transaction.TransactionType.PAYMENT) {
                // Payments are positive amounts that reduce balance
                unpaidBalance = Money.subtract(unpaidBalance, tx.getAmount());
            }
        }
        unpaidBalance = Math.max(0, unpaidBalance);

        double interest = 0.0;
        double fees = 0.0;

        if (unpaidBalance > 0) {
            // Monthly interest (APR / 12)
            double monthlyRate = Money.divide(config.getInterestRateApr(), 12.0);
            interest = Money.multiply(unpaidBalance, monthlyRate);
            
            if (isOverdue) {
                fees = config.getLateFee();
            }
        }

        statement.setFees(Money.round2(fees));
        if (interest > 0 || fees > 0) {
            // Create interest/fee transactions with positive amounts
            if (interest > 0) {
                billingService.createTransaction(
                        user.getId(),
                        "Interest Charge",
                        Money.round2(interest),
                        "Interest",
                        Transaction.TransactionType.INTEREST
                );
            }

            if (fees > 0) {
                billingService.createTransaction(
                        user.getId(),
                        "Late Fee",
                        Money.round2(fees),
                        "Fee",
                        Transaction.TransactionType.FEE
                );
            }

            // Deduct from money if available, otherwise it's added to balance
            double totalCharge = Money.add(interest, fees);
            if (res.getMoney() >= totalCharge) {
                res.setMoney(Money.subtract(res.getMoney(), totalCharge));
            }
            // If not enough money, it becomes part of the balance (credit)
        }

        if (isOverdue && statement.getStatus() == Statement.StatementStatus.OPEN) {
            statement.setStatus(Statement.StatementStatus.OVERDUE);
        }

        statementRepo.save(statement);
        resourceRepo.save(res);
    }

    /**
     * Calculate credit score with breakdown.
     * Returns a breakdown showing payment history, utilization, and age deltas.
     */
    private CreditScoreBreakdown calculateCreditScore(User user, Resource res, Statement statement) {
        double score = config.getBaseCreditScore();

        // Payment history (35% weight)
        List<Statement> allStatements = statementRepo.findByUser_IdOrderByMonthNumberDesc(user.getId());
        int onTimePayments = 0;
        int totalPayments = 0;
        int latePayments = 0;
        
        for (Statement stmt : allStatements) {
            if (stmt.getStatus() == Statement.StatementStatus.PAID) {
                totalPayments++;
                // Check if paid before due date
                List<Transaction> payments = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
                boolean onTime = false;
                for (Transaction tx : payments) {
                    if (tx.getStatement() != null && tx.getStatement().getId().equals(stmt.getId()) &&
                        tx.getType() == Transaction.TransactionType.PAYMENT) {
                        if (tx.getTimestamp().toLocalDate().isBefore(stmt.getDueDate())) {
                            onTime = true;
                            break;
                        }
                    }
                }
                if (onTime) {
                    onTimePayments++;
                } else {
                    latePayments++;
                }
            } else if (stmt.getStatus() == Statement.StatementStatus.OVERDUE) {
                latePayments++;
            }
        }
        
        double paymentHistoryRatio = totalPayments > 0 ? 
                (onTimePayments * 1.0 / (onTimePayments + latePayments)) : 0.5;
        double paymentDelta = (paymentHistoryRatio - 0.5) * 100; // -50 to +50 points
        score += paymentDelta;

        // Utilization (30% weight)
        double balance = getCurrentBalance(user);
        double utilization = res.getCreditLimit() > 0 ? 
                Money.divide(balance, res.getCreditLimit()) : 0;
        
        double utilDelta = 0.0;
        if (utilization < 0.10) {
            utilDelta = 15.0; // <10% utilization: +15 points
        } else if (utilization < 0.30) {
            utilDelta = 8.0; // 10-30%: +8 points
        } else if (utilization < 0.50) {
            utilDelta = 0.0; // 30-50%: 0 points
        } else if (utilization < 0.90) {
            utilDelta = -15.0; // 50-90%: -15 points
        } else {
            utilDelta = -35.0; // >90%: -35 points
        }
        score += utilDelta;

        // Age (10% weight)
        double historyScore = Math.min(1.0, res.getCurrentMonth() / 12.0); // Max benefit at 12 months
        double ageDelta = (historyScore - 0.5) * 20; // -10 to +10 points
        score += ageDelta;

        // Keep score in reasonable range
        score = Math.max(config.getCreditScoreMin(), Math.min(config.getCreditScoreMax(), score));

        return new CreditScoreBreakdown(
                Money.round2(score),
                Money.round2(paymentDelta),
                Money.round2(utilDelta),
                Money.round2(ageDelta)
        );
    }

    /**
     * Get current balance using normalized transaction model.
     * Balance = sum(PURCHASE + FEE + INTEREST) - sum(PAYMENT + INCOME + REWARD)
     */
    private double getCurrentBalance(User user) {
        List<Transaction> txs = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
        double balance = 0.0;
        
        for (Transaction tx : txs) {
            // All amounts are positive, so add charges and subtract credits
            if (tx.getType() == Transaction.TransactionType.PURCHASE ||
                tx.getType() == Transaction.TransactionType.INTEREST ||
                tx.getType() == Transaction.TransactionType.FEE) {
                balance = Money.add(balance, tx.getAmount());
            } else if (tx.getType() == Transaction.TransactionType.PAYMENT ||
                       tx.getType() == Transaction.TransactionType.INCOME ||
                       tx.getType() == Transaction.TransactionType.REWARD) {
                balance = Money.subtract(balance, tx.getAmount());
            }
        }
        
        return Money.round2(Math.max(0, balance));
    }

    private MonthlySummaryDTO buildMonthlySummary(User user, int monthNumber, 
                                                   double oldCreditScore, CreditScoreBreakdown breakdown,
                                                   Statement statement) {
        Resource res = resourceRepo.findByUserId(user.getId());
        MonthlySummaryDTO summary = new MonthlySummaryDTO();
        summary.oldCreditScore = oldCreditScore;
        summary.newCreditScore = breakdown.finalScore;
        summary.creditScoreDelta = Money.round2(breakdown.finalScore - oldCreditScore);
        summary.paymentHistoryDelta = breakdown.paymentDelta;
        summary.utilizationDelta = breakdown.utilDelta;
        summary.ageDelta = breakdown.ageDelta;

        // Spending summary (using normalized model)
        List<Transaction> transactions = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
        Map<String, Double> categorySpend = new HashMap<>();
        double totalSpend = 0.0;
        for (Transaction tx : transactions) {
            if (tx.getTimestamp() != null && 
                tx.getTimestamp().toLocalDate().getMonthValue() == LocalDate.now().getMonthValue() - 1) {
                if (tx.getType() == Transaction.TransactionType.PURCHASE) {
                    // All amounts are positive
                    totalSpend = Money.add(totalSpend, tx.getAmount());
                    categorySpend.put(tx.getCategory(), 
                        Money.add(categorySpend.getOrDefault(tx.getCategory(), 0.0), tx.getAmount()));
                }
            }
        }
        summary.totalSpend = Money.round2(totalSpend);
        summary.categorySpend = categorySpend;

        // Payment summary
        summary.totalPaid = 0.0;
        summary.onTimePayments = 0;
        summary.latePayments = 0;
        if (statement != null) {
            List<Transaction> payments = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
            for (Transaction tx : payments) {
                if (tx.getStatement() != null && tx.getStatement().getId().equals(statement.getId()) &&
                    tx.getType() == Transaction.TransactionType.PAYMENT) {
                    summary.totalPaid = Money.add(summary.totalPaid, tx.getAmount());
                    if (tx.getTimestamp().toLocalDate().isBefore(statement.getDueDate())) {
                        summary.onTimePayments++;
                    } else {
                        summary.latePayments++;
                    }
                }
            }
        }

        // Interest and fees - calculate from transactions in the statement period
        double interestCharged = 0.0;
        double feesCharged = 0.0;
        if (statement != null) {
            List<Transaction> statementTransactions = txRepo.findByUser_IdOrderByTimestampDesc(user.getId());
            for (Transaction tx : statementTransactions) {
                if (tx.getStatement() != null && tx.getStatement().getId().equals(statement.getId())) {
                    if (tx.getType() == Transaction.TransactionType.INTEREST) {
                        interestCharged = Money.add(interestCharged, tx.getAmount());
                    } else if (tx.getType() == Transaction.TransactionType.FEE) {
                        feesCharged = Money.add(feesCharged, tx.getAmount());
                    }
                }
            }
        }
        summary.interestCharged = Money.round2(interestCharged);
        summary.feesCharged = Money.round2(feesCharged);

        // Tips
        summary.tips = new java.util.ArrayList<>();
        if (breakdown.finalScore < oldCreditScore) {
            summary.tips.add("Your credit score decreased. Consider paying bills on time and reducing utilization.");
        }
        if (summary.latePayments > 0) {
            summary.tips.add("You made late payments this month. On-time payments are crucial for credit health.");
        }
        double utilization = res.getCreditLimit() > 0 ? 
            Money.divide(getCurrentBalance(user), res.getCreditLimit()) : 0;
        if (utilization > 0.3) {
            summary.tips.add("Your credit utilization is high (" + 
                String.format("%.0f%%", utilization * 100) + 
                "). Try to keep it below 30%.");
        }

        return summary;
    }

    /**
     * Credit score breakdown for transparency.
     */
    private static class CreditScoreBreakdown {
        final double finalScore;
        final double paymentDelta;
        final double utilDelta;
        final double ageDelta;

        CreditScoreBreakdown(double finalScore, double paymentDelta, double utilDelta, double ageDelta) {
            this.finalScore = finalScore;
            this.paymentDelta = paymentDelta;
            this.utilDelta = utilDelta;
            this.ageDelta = ageDelta;
        }
    }

    public static class MonthlySummaryDTO {
        public double oldCreditScore;
        public double newCreditScore;
        public double creditScoreDelta;
        public double paymentHistoryDelta;
        public double utilizationDelta;
        public double ageDelta;
        public double totalSpend;
        public Map<String, Double> categorySpend;
        public double totalPaid;
        public int onTimePayments;
        public int latePayments;
        public double interestCharged;
        public double feesCharged;
        public List<String> tips;
    }
}
