package com.das.skillmatrix.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.RefreshTokenRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.LoginResponse;
import com.das.skillmatrix.dto.response.RefreshTokenResponse;
import com.das.skillmatrix.security.JwtUtil;
import com.das.skillmatrix.service.AuthService;

import jakarta.security.auth.message.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
	@Value("${jwt.refresh.expiration}")
	private long refreshExpiration;

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request)
            throws AuthException {

        LoginResponse response = authService.login(request);
        // Set refresh_token into cookie
        ResponseCookie springCookie = ResponseCookie.from("refresh_token", response.getRefreshToken()).httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshExpiration)
                .build();
        return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.SET_COOKIE, springCookie.toString()).body(new ApiResponse<>(response, true, null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) throws AuthException {

        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication) throws AuthException{
    	if (authentication == null) {
    		throw new AuthException("Unauthorized");
    	}
    	String response = authService.logout(authentication.getName());
    	// Set refresh_token into cookie
        ResponseCookie springCookie = ResponseCookie.from("refresh_token", null).httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
    	return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.SET_COOKIE, springCookie.toString()).body(new ApiResponse<>(response,true,null));
    }
}
