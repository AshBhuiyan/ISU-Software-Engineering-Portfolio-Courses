package onetoone.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the CyCredit game economy.
 * All values can be overridden in application.properties.
 * Bean is registered via @EnableConfigurationProperties in Main.java
 */
@ConfigurationProperties(prefix = "cycredit")
public class GameConfig {
    
    // Game settings
    private int maxTurnsPerMonth = 10;
    
    // Credit score settings
    private double baseCreditScore = 700.0;
    private double creditScoreMin = 300.0;
    private double creditScoreMax = 850.0;
    
    // Billing settings
    private double interestRateApr = 0.18; // 18% APR
    private double lateFee = 25.0;
    private double minimumPaymentPercent = 0.02; // 2%
    private double minimumPaymentFloor = 5.0;  // Reduced from $25 to $5 for demo
    private int statementGracePeriodDays = 21;
    
    // Credit limit
    private double defaultCreditLimit = 1500.0;
    
    // Job settings
    private long jobMinDurationMs = 5000; // 5 seconds (reduced from 30s for demo)
    private double jobBasePayout = 50.0;
    private double jobStreakBonusPercent = 0.10; // 10% per streak, max 30%
    private int jobStreakMax = 3;
    private int jobSoftCapPasses = 10; // After 10 passes per hour
    private double jobSoftCapReduction = 0.40; // 40% reduction
    
    // Library settings
    private double libraryRewardMoney = 5.0;
    private int libraryRewardXp = 10;
    
    // Wellness settings
    private double wellnessRewardMoney = 10.0;
    
    // Getters and Setters
    public int getMaxTurnsPerMonth() { return maxTurnsPerMonth; }
    public void setMaxTurnsPerMonth(int maxTurnsPerMonth) { this.maxTurnsPerMonth = maxTurnsPerMonth; }
    
    public double getBaseCreditScore() { return baseCreditScore; }
    public void setBaseCreditScore(double baseCreditScore) { this.baseCreditScore = baseCreditScore; }
    
    public double getCreditScoreMin() { return creditScoreMin; }
    public void setCreditScoreMin(double creditScoreMin) { this.creditScoreMin = creditScoreMin; }
    
    public double getCreditScoreMax() { return creditScoreMax; }
    public void setCreditScoreMax(double creditScoreMax) { this.creditScoreMax = creditScoreMax; }
    
    public double getInterestRateApr() { return interestRateApr; }
    public void setInterestRateApr(double interestRateApr) { this.interestRateApr = interestRateApr; }
    
    public double getLateFee() { return lateFee; }
    public void setLateFee(double lateFee) { this.lateFee = lateFee; }
    
    public double getMinimumPaymentPercent() { return minimumPaymentPercent; }
    public void setMinimumPaymentPercent(double minimumPaymentPercent) { this.minimumPaymentPercent = minimumPaymentPercent; }
    
    public double getMinimumPaymentFloor() { return minimumPaymentFloor; }
    public void setMinimumPaymentFloor(double minimumPaymentFloor) { this.minimumPaymentFloor = minimumPaymentFloor; }
    
    public int getStatementGracePeriodDays() { return statementGracePeriodDays; }
    public void setStatementGracePeriodDays(int statementGracePeriodDays) { this.statementGracePeriodDays = statementGracePeriodDays; }
    
    public double getDefaultCreditLimit() { return defaultCreditLimit; }
    public void setDefaultCreditLimit(double defaultCreditLimit) { this.defaultCreditLimit = defaultCreditLimit; }
    
    public long getJobMinDurationMs() { return jobMinDurationMs; }
    public void setJobMinDurationMs(long jobMinDurationMs) { this.jobMinDurationMs = jobMinDurationMs; }
    
    public double getJobBasePayout() { return jobBasePayout; }
    public void setJobBasePayout(double jobBasePayout) { this.jobBasePayout = jobBasePayout; }
    
    public double getJobStreakBonusPercent() { return jobStreakBonusPercent; }
    public void setJobStreakBonusPercent(double jobStreakBonusPercent) { this.jobStreakBonusPercent = jobStreakBonusPercent; }
    
    public int getJobStreakMax() { return jobStreakMax; }
    public void setJobStreakMax(int jobStreakMax) { this.jobStreakMax = jobStreakMax; }
    
    public int getJobSoftCapPasses() { return jobSoftCapPasses; }
    public void setJobSoftCapPasses(int jobSoftCapPasses) { this.jobSoftCapPasses = jobSoftCapPasses; }
    
    public double getJobSoftCapReduction() { return jobSoftCapReduction; }
    public void setJobSoftCapReduction(double jobSoftCapReduction) { this.jobSoftCapReduction = jobSoftCapReduction; }
    
    public double getLibraryRewardMoney() { return libraryRewardMoney; }
    public void setLibraryRewardMoney(double libraryRewardMoney) { this.libraryRewardMoney = libraryRewardMoney; }
    
    public int getLibraryRewardXp() { return libraryRewardXp; }
    public void setLibraryRewardXp(int libraryRewardXp) { this.libraryRewardXp = libraryRewardXp; }
    
    public double getWellnessRewardMoney() { return wellnessRewardMoney; }
    public void setWellnessRewardMoney(double wellnessRewardMoney) { this.wellnessRewardMoney = wellnessRewardMoney; }
}

