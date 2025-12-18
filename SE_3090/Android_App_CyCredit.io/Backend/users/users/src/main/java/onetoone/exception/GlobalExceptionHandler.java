package onetoone.exception;

import onetoone.util.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Global exception handler for consistent error responses across all controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException e) {
        String message = e.getMessage();
        String errorCode;
        
        if (message != null && message.contains("NO_TURNS")) {
            errorCode = ApiError.NO_TURNS;
        } else if (message != null && message.contains("INSUFFICIENT_FUNDS")) {
            errorCode = ApiError.INSUFFICIENT_FUNDS;
        } else if (message != null && message.contains("OUT_OF_CREDIT")) {
            errorCode = ApiError.OUT_OF_CREDIT;
        } else if (message != null && message.contains("Resource not found")) {
            errorCode = ApiError.RESOURCE_NOT_FOUND;
        } else {
            errorCode = ApiError.BAD_REQUEST;
        }
        
        return ResponseEntity.badRequest()
                .body(new ApiError(errorCode, message != null ? message : "Invalid operation"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError(ApiError.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException e) {
        // Log the full exception for debugging
        e.printStackTrace();
        System.err.println("RuntimeException caught: " + e.getClass().getName());
        System.err.println("Message: " + e.getMessage());
        
        String message = e.getMessage();
        
        // Handle LazyInitializationException (Hibernate lazy loading issues)
        if (e.getClass().getName().contains("LazyInitializationException") || 
            message != null && message.contains("could not initialize proxy")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(ApiError.BAD_REQUEST, 
                        "Database session error. Please try again. Details: " + 
                        (message != null ? message : e.getClass().getSimpleName())));
        }
        
        if (message != null && message.contains("not found")) {
            if (message.contains("User")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(ApiError.USER_NOT_FOUND, message));
            } else if (message.contains("Statement")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(ApiError.STATEMENT_NOT_FOUND, message));
            }
        }
        
        // Default to 500 for unexpected runtime exceptions - but include actual error
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(ApiError.BAD_REQUEST, 
                    "An unexpected error occurred: " + 
                    (message != null ? message : e.getClass().getSimpleName())));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        // This happens when concurrent turn consumption conflicts
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ApiError.NO_TURNS, 
                    "Turn consumption conflict. Please try again."));
    }
}

