package onetoone.billing;

import onetoone.Resource.Resource;
import onetoone.Resource.ResourceRepository;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import onetoone.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * BillingService handles all transaction operations.
 * 
 * TRANSACTION MODEL (Option A - All Positive):
 * - All transaction amounts are stored as POSITIVE values
 * - Balance = sum(PURCHASE + FEE + INTEREST) - sum(PAYMENT + INCOME + REWARD)
 * - Purchases, fees, and interest increase balance (positive amounts)
 * - Payments, income, and rewards decrease balance (positive amounts)
 */
@Service
public class BillingService {

    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final ResourceRepository resourceRepo;

    public BillingService(TransactionRepository txRepo, UserRepository userRepo, ResourceRepository resourceRepo) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.resourceRepo = resourceRepo;
    }

    /**
     * Get current balance and unbilled monthly spend.
     * 
     * Balance = sum(PURCHASE + FEE + INTEREST) - sum(PAYMENT + INCOME + REWARD) for ALL transactions
     * MonthlySpend = sum of PURCHASE transactions where statement is NULL (unbilled activity)
     * 
     * This ensures monthlySpend represents "Current Unbilled Activity" and will
     * naturally reset to 0 once a statement is generated and transactions are linked.
     */
    public SummaryDTO getSummary(int userId) {
        List<Transaction> txs = txRepo.findByUser_IdOrderByTimestampDesc(userId);
        double balance = 0.0;
        double monthlySpend = 0.0;

        for (Transaction t : txs) {
            // All amounts are positive, so we add charges and subtract credits
            if (t.getType() == Transaction.TransactionType.PURCHASE ||
                t.getType() == Transaction.TransactionType.INTEREST ||
                t.getType() == Transaction.TransactionType.FEE) {
                balance = Money.add(balance, t.getAmount());
            } else if (t.getType() == Transaction.TransactionType.PAYMENT ||
                       t.getType() == Transaction.TransactionType.INCOME ||
                       t.getType() == Transaction.TransactionType.REWARD) {
                balance = Money.subtract(balance, t.getAmount());
            }

            // Calculate monthly spend = UNBILLED purchases only (statement == null)
            // This naturally resets when a statement is generated and transactions are linked
            if (t.getType() == Transaction.TransactionType.PURCHASE &&
                t.getStatement() == null) {
                monthlySpend = Money.add(monthlySpend, t.getAmount());
            }
        }

        Resource res = resourceRepo.findByUserId(userId);
        double creditLimit = res != null ? res.getCreditLimit() : 1500.0;

        return new SummaryDTO(Money.round2(balance), Money.round2(monthlySpend), creditLimit);
    }

    public List<Transaction> listTransactions(int userId) {
        return txRepo.findByUser_IdOrderByTimestampDesc(userId);
    }

    /**
     * Apply a charge (purchase, fee, or interest).
     * Amount must be positive and will be stored as positive.
     */
    @Transactional
    public void applyCharge(int userId, String merchant, double total, String category, OffsetDateTime when) {
        applyCharge(userId, merchant, total, category, when, null);
    }
    
    public void applyCharge(int userId, String merchant, double total, String category, OffsetDateTime when, String purchaseNonce) {
        Money.validatePositive(total);
        
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Resource res = resourceRepo.findByUserId(userId);
        if (res == null) {
            throw new IllegalStateException("Resource not found for user " + userId);
        }

        // Check if user has enough money or credit
        double currentBalance = getCurrentBalance(userId);
        if (res.getMoney() < total && (currentBalance + total) > res.getCreditLimit()) {
            throw new IllegalStateException("OUT_OF_CREDIT");
        }

        // Create transaction with positive amount
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setMerchant(merchant);
        tx.setCategory(category != null ? category : "Purchase");
        tx.setAmount(Money.round2(total)); // Store as positive
        tx.setType(Transaction.TransactionType.PURCHASE);
        tx.setTimestamp(when != null ? when : OffsetDateTime.now());
        tx.setPurchaseNonce(purchaseNonce); // Set nonce for idempotency
        txRepo.save(tx);

        // Deduct from money if available, otherwise it goes to balance
        if (res.getMoney() >= total) {
            res.setMoney(Money.subtract(res.getMoney(), total));
            resourceRepo.save(res);
        }
        // If not enough money, the charge goes to the balance (credit)
    }

    /**
     * Apply a payment. Amount must be positive and will be stored as positive.
     */
    @Transactional
    public void applyPayment(int userId, double amount, OffsetDateTime when) {
        Money.validatePositive(amount);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Resource res = resourceRepo.findByUserId(userId);
        if (res == null) {
            throw new IllegalStateException("Resource not found for user " + userId);
        }

        // Check if user has enough money
        if (res.getMoney() < amount) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }

        // Create payment transaction with positive amount
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setMerchant("Statement Payment");
        tx.setCategory("Payment");
        tx.setAmount(Money.round2(amount)); // Store as positive
        tx.setType(Transaction.TransactionType.PAYMENT);
        tx.setTimestamp(when != null ? when : OffsetDateTime.now());
        txRepo.save(tx);

        // Deduct money
        res.setMoney(Money.subtract(res.getMoney(), amount));
        resourceRepo.save(res);
    }

    /**
     * Get current TOTAL balance for a user (all transactions, billed or unbilled).
     */
    public double getCurrentBalance(int userId) {
        List<Transaction> txs = txRepo.findByUser_IdOrderByTimestampDesc(userId);
        double balance = 0.0;
        
        for (Transaction tx : txs) {
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
    
    /**
     * Get UNBILLED balance for a user (only transactions where statement == null).
     * This is what should appear on the next generated statement.
     */
    public double getUnbilledBalance(int userId) {
        List<Transaction> txs = txRepo.findByUser_IdOrderByTimestampDesc(userId);
        double balance = 0.0;
        
        for (Transaction tx : txs) {
            // Only consider unbilled transactions
            if (tx.getStatement() != null) {
                continue;
            }
            
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

    /**
     * Create a transaction with normalized amount (always positive).
     * The type determines whether it increases or decreases balance.
     */
    public Transaction createTransaction(int userId, String merchant, double amount, String category) {
        return createTransaction(userId, merchant, amount, category, Transaction.TransactionType.PURCHASE);
    }

    /**
     * Find transaction by purchaseNonce for idempotency checks.
     */
    public java.util.Optional<Transaction> findTransactionByPurchaseNonce(String purchaseNonce) {
        if (purchaseNonce == null || purchaseNonce.isEmpty()) {
            return java.util.Optional.empty();
        }
        return txRepo.findByPurchaseNonce(purchaseNonce);
    }
    
    /**
     * Create a transaction with normalized amount (always positive).
     * Converts negative amounts to positive (normalized model).
     */
    public Transaction createTransaction(int userId, String merchant, double amount, String category, Transaction.TransactionType type) {
        // Normalize: convert negative to positive (all amounts stored as positive)
        double normalizedAmount = Math.abs(amount);
        Money.validatePositive(normalizedAmount);
        
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setMerchant(merchant);
        tx.setAmount(Money.round2(normalizedAmount)); // Always store as positive
        tx.setCategory(category);
        tx.setType(type);
        tx.setTimestamp(OffsetDateTime.now());

        return txRepo.save(tx);
    }

    public Transaction updateTransaction(long id, String merchant, double amount, String category) {
        Money.validatePositive(amount);
        
        Transaction tx = txRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));

        tx.setMerchant(merchant);
        tx.setAmount(Money.round2(amount)); // Always store as positive
        tx.setCategory(category);
        tx.setTimestamp(OffsetDateTime.now());

        return txRepo.save(tx);
    }

    public void deleteTransaction(long id) {
        if (!txRepo.existsById(id)) {
            throw new RuntimeException("Transaction not found: " + id);
        }
        txRepo.deleteById(id);
    }

    public static class SummaryDTO {
        public double balance, monthlySpend, creditLimit;
        public SummaryDTO(double b, double m, double c) {
            balance = b;
            monthlySpend = m;
            creditLimit = c;
        }
    }
}
