package onetoone.util;

/**
 * Standard API error response structure.
 */
public class ApiError {
    private String error;
    private String message;
    
    public ApiError(String error, String message) {
        this.error = error;
        this.message = message;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // Common error codes
    public static final String NO_TURNS = "NO_TURNS";
    public static final String INVALID_DURATION = "INVALID_DURATION";
    public static final String DUPLICATE_NONCE = "DUPLICATE_NONCE";
    public static final String MAX_ITEMS = "MAX_ITEMS";
    public static final String ALREADY_MASTERED = "ALREADY_MASTERED";
    public static final String ALREADY_CLAIMED = "ALREADY_CLAIMED";
    public static final String PAYMENT_TOO_LOW = "PAYMENT_TOO_LOW";
    public static final String PAYMENT_TOO_HIGH = "PAYMENT_TOO_HIGH";
    public static final String STATEMENT_NOT_FOUND = "STATEMENT_NOT_FOUND";
    public static final String OUT_OF_CREDIT = "OUT_OF_CREDIT";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ROOM_CAP_REACHED = "ROOM_CAP_REACHED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String NOT_COMPLETED = "NOT_COMPLETED";
    public static final String MEMBERSHIP_REQUIRED = "MEMBERSHIP_REQUIRED";
    public static final String DAILY_LIMIT_REACHED = "DAILY_LIMIT_REACHED";
}

