
package onetoone.billing;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

import onetoone.Users.User;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum TransactionType {
        PURCHASE, PAYMENT, INCOME, INTEREST, FEE, REWARD
    }

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column
 private String merchant;
 @Column
 private double amount;
     @Column
     private String category;
 @Column
 private OffsetDateTime timestamp;

 @Enumerated(EnumType.STRING)
 @Column(name = "transaction_type")
 private TransactionType type;

 @ManyToOne(fetch = FetchType.LAZY, optional = false)
 @JoinColumn(name = "user_id")
 private User user;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "statement_id")
 private Statement statement;

 @Column(name = "purchase_nonce", unique = true, nullable = true)
 private String purchaseNonce;

     public Transaction() {}

 public Transaction(User user, String merchant, double amount, String category, OffsetDateTime timestamp) {
 this.user = user;
 this.merchant = merchant;
 this.amount = amount;
 this.category = category;
 this.timestamp = timestamp;
 this.type = TransactionType.PURCHASE; // Default
 }

 public Transaction(User user, String merchant, double amount, String category, OffsetDateTime timestamp, TransactionType type) {
 this.user = user;
 this.merchant = merchant;
 this.amount = amount;
 this.category = category;
 this.timestamp = timestamp;
 this.type = type;
 }

 public Long getId() { return id; }
 public String getMerchant() { return merchant; }
 public double getAmount() { return amount; }
 public String getCategory() { return category; }
 public OffsetDateTime getTimestamp() { return timestamp; }
 public User getUser() { return user; }

 public void setUser(User user) { this.user = user; }
 public void setMerchant(String merchant) { this.merchant = merchant; }
 public void setAmount(double amount) { this.amount = amount; }
 public void setCategory(String category) { this.category = category; }
 public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
 public TransactionType getType() { return type; }
 public void setType(TransactionType type) { this.type = type; }
 public Statement getStatement() { return statement; }
 public void setStatement(Statement statement) { this.statement = statement; }
 public String getPurchaseNonce() { return purchaseNonce; }
 public void setPurchaseNonce(String purchaseNonce) { this.purchaseNonce = purchaseNonce; }
}