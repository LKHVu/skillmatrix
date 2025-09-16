package com.das.skillmatrix.exception;

import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), 401);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        log.error("Unexpected error - Error: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred: " + e.getMessage(), 500);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
