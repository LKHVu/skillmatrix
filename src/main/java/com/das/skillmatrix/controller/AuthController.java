package com.das.skillmatrix.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.RefreshTokenRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.ErrorResponse;
import com.das.skillmatrix.dto.response.LoginResponse;
import com.das.skillmatrix.dto.response.RefreshTokenResponse;
import com.das.skillmatrix.service.AuthService;

import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {

        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(new ApiResponse<>(response, true, null));
        } catch (AuthException e) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(null,
                            false,
                            new ErrorResponse(
                                    e.getMessage(),
                                    401)));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null,
                            false,
                            new ErrorResponse(
                                    e.getMessage(),
                                    500)));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        try {
            RefreshTokenResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(new ApiResponse<>(response, true, null));
        } catch (AuthException e) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(null,
                            false,
                            new ErrorResponse(
                                    e.getMessage(),
                                    401)));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(null,
                            false,
                            new ErrorResponse(
                                    e.getMessage(),
                                    500)));
        }
    }
}
