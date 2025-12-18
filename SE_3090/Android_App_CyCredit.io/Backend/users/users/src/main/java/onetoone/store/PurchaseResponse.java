package onetoone.store;

public class PurchaseResponse {
    private String status;
    private double newBalance;

    public PurchaseResponse() {}
    public PurchaseResponse(String status, double newBalance) {
        this.status = status;
        this.newBalance = newBalance;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getNewBalance() { return newBalance; }
    public void setNewBalance(double newBalance) { this.newBalance = newBalance; }
}
