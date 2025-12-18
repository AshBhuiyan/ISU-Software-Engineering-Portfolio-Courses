package onetoone.billing;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "statements")
public class Statement {

    public enum StatementStatus {
        OPEN, PAID, OVERDUE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "month_number")
    private int monthNumber;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "statement_date")
    private LocalDate statementDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "total_due")
    private double totalDue;

    @Column(name = "minimum_due")
    private double minimumDue;

    @Column(name = "interest_rate")
    private double interestRate;

    @Column(name = "fees")
    private double fees;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatementStatus status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Statement() {
        this.status = StatementStatus.OPEN;
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getMonthNumber() { return monthNumber; }
    public void setMonthNumber(int monthNumber) { this.monthNumber = monthNumber; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getTotalDue() { return totalDue; }
    public void setTotalDue(double totalDue) { this.totalDue = totalDue; }

    public double getMinimumDue() { return minimumDue; }
    public void setMinimumDue(double minimumDue) { this.minimumDue = minimumDue; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getFees() { return fees; }
    public void setFees(double fees) { this.fees = fees; }

    public StatementStatus getStatus() { return status; }
    public void setStatus(StatementStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

