package onetoone.billing;

public class SummaryDTO {
    private double balance;
    private double monthlySpend;
    private double creditLimit;

    public SummaryDTO() {}

    public SummaryDTO(double balance, double monthlySpend, double creditLimit) {
        this.balance = balance;
        this.monthlySpend = monthlySpend;
        this.creditLimit = creditLimit;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getMonthlySpend() { return monthlySpend; }
    public void setMonthlySpend(double monthlySpend) { this.monthlySpend = monthlySpend; }
    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }
}
