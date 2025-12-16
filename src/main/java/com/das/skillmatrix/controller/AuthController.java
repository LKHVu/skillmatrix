package com.das.skillmatrix.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.LogoutRequest;
import com.das.skillmatrix.dto.request.RefreshTokenRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
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

    private final AuthService authService = null;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request)
            throws AuthException {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) throws AuthException {

        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) throws AuthException{
    	String response = authService.logout(request);
    	return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }
}
