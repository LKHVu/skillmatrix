package com.das.skillmatrix.service;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.RefreshTokenRequest;
import com.das.skillmatrix.dto.response.LoginResponse;
import com.das.skillmatrix.dto.response.RefreshTokenResponse;
import com.das.skillmatrix.entity.RefreshToken;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.RefreshTokenRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.security.JwtUtil;
import jakarta.security.auth.message.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setEmail("user@example.com");
        mockUser.setPasswordHash("encoded-password");
        mockUser.setRole("USER");
    }

    @Test
    @DisplayName("login() should return tokens when credentials are valid")
    void login_shouldReturnTokens_whenCredentialsValid() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "password");

        when(userRepository.findUserByEmail("user@example.com")).thenReturn(mockUser);
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken("user@example.com", "USER")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user@example.com")).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login() should throw AuthException when email not found")
    void login_shouldThrowException_whenEmailNotFound() {
        LoginRequest request = new LoginRequest("notfound@example.com", "password");

        when(userRepository.findUserByEmail("notfound@example.com")).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("login() should throw AuthException when password is wrong")
    void login_shouldThrowException_whenWrongPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong");

        when(userRepository.findUserByEmail("user@example.com")).thenReturn(mockUser);
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("WRONG_PASSWORD", ex.getMessage());
    }

    @Test
    @DisplayName("refreshToken() should return new access token when refresh token is valid")
    void refreshToken_shouldReturnNewAccessToken_whenValid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtUtil.validateRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid-refresh-token")).thenReturn("user@example.com");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(mockUser);
        when(jwtUtil.generateAccessToken("user@example.com", "USER")).thenReturn("new-access-token");

        RefreshTokenResponse response = authService.refreshToken(request);

        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    @DisplayName("refreshToken() should throw AuthException when token invalid")
    void refreshToken_shouldThrowException_whenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        when(jwtUtil.validateRefreshToken("invalid-token")).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", ex.getMessage());
    }
    
    @Test
    @DisplayName("logout() should return success when token is valid")
    void logout_shoutReturnSuccess_whenTokenIsValid() throws AuthException {
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@example.com"); 
        when(jwtUtil.validateAccessToken("valid-token", "user@example.com")).thenReturn(true);
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(mockUser);

        String result = authService.logout("valid-token");

        assertEquals("Logout Success", result);

        verify(refreshTokenRepository, times(1)).deleteByUser(mockUser);
    }
    
    @Test
    @DisplayName("logout() should throw AuthException when token is invalid")
    void logout_shouldThrowException_whenTokenIsInvalid(){
    	when(jwtUtil.extractEmail("invalid-token")).thenReturn("user@example.com");
    	when(jwtUtil.validateAccessToken("invalid-token", "user@example.com")).thenReturn(false);
    	
    	AuthException ex = assertThrows(AuthException.class, () -> authService.logout("invalid-token"));
    	assertEquals("Invalid or expired access token", ex.getMessage());
    	
    	verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
    }
}
