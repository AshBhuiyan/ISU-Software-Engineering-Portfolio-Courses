package cycredit.io.model;

import org.json.JSONObject;

/**
 * Transaction model matching backend normalized transaction model.
 * All amounts are positive. Type determines if it increases or decreases balance.
 */
public class Transaction {
    private final String merchant;
    private final double amount; // Always positive (normalized model)
    private final String isoTimestamp;
    private final String category;
    private final String type; // PURCHASE, PAYMENT, INCOME, INTEREST, FEE, REWARD

    public Transaction(String merchant, double amount, String isoTimestamp, String category, String type) {
        this.merchant = merchant;
        this.amount = Math.abs(amount); // Ensure positive
        this.isoTimestamp = isoTimestamp;
        this.category = category;
        this.type = type != null ? type : "PURCHASE";
    }

    // Legacy constructor for backward compatibility
    public Transaction(String merchant, double amount, String isoTimestamp, String category) {
        this(merchant, amount, isoTimestamp, category, "PURCHASE");
    }

    public static Transaction fromJson(JSONObject o) {
        String merchant = o.optString("merchant", "Unknown");
        double amount = Math.abs(o.optDouble("amount", 0.0)); // Always positive
        String ts = o.optString("timestamp", "1970-01-01T00:00:00Z");
        String cat = o.optString("category", "Other");
        String type = o.optString("type", "PURCHASE");
        return new Transaction(merchant, amount, ts, cat, type);
    }

    public String getMerchant() { return merchant; }
    public double getAmount() { return amount; }
    public String getIsoTimestamp() { return isoTimestamp; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    
    /**
     * Returns true if this transaction increases balance (charge).
     * PURCHASE, INTEREST, FEE increase balance.
     */
    public boolean isCharge() {
        return "PURCHASE".equals(type) || "INTEREST".equals(type) || "FEE".equals(type);
    }
    
    /**
     * Returns true if this transaction decreases balance (credit).
     * PAYMENT, INCOME, REWARD decrease balance.
     */
    public boolean isCredit() {
        return "PAYMENT".equals(type) || "INCOME".equals(type) || "REWARD".equals(type);
    }
}