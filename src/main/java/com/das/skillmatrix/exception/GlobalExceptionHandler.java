package com.das.skillmatrix.exception;

import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.ErrorResponse;
import jakarta.security.auth.message.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthException(AuthException e) {
        log.error("Authentication failed - Error: {}", e.getMessage());

        int code = 401;
        String userMessage;
        switch (e.getMessage()) {
            case "ACCOUNT_NOT_FOUND":
                userMessage = "We can’t find your account!";
                break;
            case "WRONG_PASSWORD":
                userMessage = "Wrong password!";
                break;
            default:
                userMessage = "Invalid credentials";
        }

        ErrorResponse errorResponse = new ErrorResponse(userMessage, code);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation failed - {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse("We can’t find your account!", 400);
        ApiResponse<Object> response = new ApiResponse<>(errors, false, errorResponse);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        log.error("Unexpected error - Error: {}", e.getMessage(), e);

        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred: " + e.getMessage(), 500);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
