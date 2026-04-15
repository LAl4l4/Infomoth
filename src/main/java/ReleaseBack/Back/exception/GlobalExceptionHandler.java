package ReleaseBack.Back.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UnauthorizedError.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedError e) {
        return ResponseEntity
                .status(e.getCode())
                .body(e.getMessage());
    }
}
