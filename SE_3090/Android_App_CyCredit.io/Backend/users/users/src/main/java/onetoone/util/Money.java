package onetoone.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for money calculations using BigDecimal to avoid floating-point errors.
 * All amounts are stored as positive values in the database.
 */
public class Money {
    
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Round a double to 2 decimal places using BigDecimal.
     */
    public static double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(SCALE, ROUNDING_MODE)
                .doubleValue();
    }
    
    /**
     * Round a BigDecimal to 2 decimal places.
     */
    public static BigDecimal round2(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Add two money values using BigDecimal.
     */
    public static double add(double a, double b) {
        return round2(BigDecimal.valueOf(a).add(BigDecimal.valueOf(b))).doubleValue();
    }
    
    /**
     * Subtract two money values using BigDecimal.
     */
    public static double subtract(double a, double b) {
        return round2(BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b))).doubleValue();
    }
    
    /**
     * Multiply a money value by a multiplier using BigDecimal.
     */
    public static double multiply(double amount, double multiplier) {
        return round2(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(multiplier))).doubleValue();
    }
    
    /**
     * Divide a money value by a divisor using BigDecimal.
     */
    public static double divide(double amount, double divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return round2(BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING_MODE)).doubleValue();
    }
    
    /**
     * Validate that an amount is positive and non-zero.
     */
    public static void validatePositive(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }
    
    /**
     * Validate that an amount is non-negative.
     */
    public static void validateNonNegative(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative: " + amount);
        }
    }
}

