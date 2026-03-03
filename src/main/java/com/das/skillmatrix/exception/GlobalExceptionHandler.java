package com.das.skillmatrix.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.ErrorResponse;

import jakarta.security.auth.message.AuthException;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Bad request - Error: {}", e.getMessage());

        String userMessage;
        HttpStatus status;

        switch (e.getMessage()) {
            case "CAREER_NOT_FOUND":
                userMessage = "Career not found";
                status = HttpStatus.NOT_FOUND;
                break;
            case "CAREER_NAME_EXISTS":
                userMessage = "Career name already exists";
                status = HttpStatus.CONFLICT;
                break;
            case "CAREER_NOT_FOUND_OR_INACTIVE":
                userMessage = "Career not found or not active";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "DEPARTMENT_NAME_EXISTS_IN_CAREER":
                userMessage = "Department name already exists in this career";
                status = HttpStatus.CONFLICT;
                break;
            case "DEPARTMENT_NOT_FOUND":
                userMessage = "Department not found";
                status = HttpStatus.NOT_FOUND;
                break;
            case "DEPARTMENT_NOT_ACTIVE":
                userMessage = "Department is not active";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "TARGET_CAREER_NOT_FOUND_OR_INACTIVE":
                userMessage = "Target career not found or not active";
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                userMessage = e.getMessage() != null ? e.getMessage() : "Bad request";
                status = HttpStatus.BAD_REQUEST;
        }

        ErrorResponse errorResponse = new ErrorResponse(userMessage, status.value());
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException e) {
        log.error("Access denied - Error: {}", e.getMessage());

        String userMessage;
        switch (e.getMessage()) {
            case "ACCESS_DENIED_TO_MIGRATE_CAREER":
                userMessage = "You do not have permission to migrate this department to the target career";
                break;
            default:
                userMessage = "Access denied";
        }

        ErrorResponse errorResponse = new ErrorResponse(userMessage, HttpStatus.FORBIDDEN.value());
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation failed - {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("Resource not found - Error: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), 404);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
