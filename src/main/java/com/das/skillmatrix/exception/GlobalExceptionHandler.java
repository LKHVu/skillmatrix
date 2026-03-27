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
import org.springframework.web.bind.MissingServletRequestParameterException;

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
            case "TEAM_NOT_FOUND":
                userMessage = "Team not found";
                status = HttpStatus.NOT_FOUND;
                break;
            case "TEAM_NOT_ACTIVE":
                userMessage = "Team is not active";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "USER_NOT_FOUND":
                userMessage = "User not found";
                status = HttpStatus.NOT_FOUND;
                break;
            case "USER_NOT_ACTIVE":
                userMessage = "Only active users can be added to team";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "USER_NOT_IN_SAME_CAREER":
                userMessage = "User does not belong to this career";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "USER_ALREADY_IN_TEAM":
                userMessage = "User already exists in this team";
                status = HttpStatus.CONFLICT;
                break;
            case "POSITION_NOT_FOUND":
                userMessage = "Position not found";
                status = HttpStatus.NOT_FOUND;
                break;
            case "TEAM_MEMBER_NOT_FOUND":
                userMessage = "Team member not found";
                status = HttpStatus.NOT_FOUND;
                break;
            case "EMAIL_ALREADY_EXISTS":
                userMessage = "Email already exists";
                status = HttpStatus.CONFLICT;
                break;
            case "EMAIL_REQUIRED":
                userMessage = "Email is required";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "INVALID_EMAIL_FORMAT":
                userMessage = "Invalid email format";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "INVALID_ROLE":
                userMessage = "Invalid role assignment";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "ROLE_EXCEEDS_PERMISSION":
                userMessage = "Cannot assign role that exceeds your permission level";
                status = HttpStatus.FORBIDDEN;
                break;
            case "SCOPE_REQUIRED":
                userMessage = "Scope assignment is required for this role";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "INVALID_SCOPE":
                userMessage = "Selected scope is not valid or not active";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "POSITION_REQUIRED":
                userMessage = "At least one position must be selected";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "POSITION_NOT_ACTIVE":
                userMessage = "One or more selected positions are not active";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "CANNOT_DEACTIVE_DEACTIVED_USER":
                userMessage = "User is already deactivated";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "DEACTIVATION_DURATION_REQUIRED":
                userMessage = "Please select a deactivation duration";
                status = HttpStatus.BAD_REQUEST;
                break;
            case "USER_NOT_DEACTIVE":
                userMessage = "Cannot reactivate: user is not deactivated";
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

        ErrorResponse errorResponse = new ErrorResponse("Validation failed", 400, errors);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(
            MissingServletRequestParameterException e) {
        log.error("Missing request parameter - Error: {}", e.getMessage());

        String userMessage = "Missing required parameter: " + e.getParameterName();

        ErrorResponse errorResponse = new ErrorResponse(userMessage, 400);
        ApiResponse<Object> response = new ApiResponse<>(null, false, errorResponse);

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
