package com.das.skillmatrix.controller;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.RefreshTokenRequest;
import com.das.skillmatrix.dto.response.LoginResponse;
import com.das.skillmatrix.dto.response.RefreshTokenResponse;
import com.das.skillmatrix.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.security.auth.message.AuthException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
    properties = {
        "jwt.refresh.expiration=604800",
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login should return 200 and tokens when credentials are valid")
    void login_shouldReturnTokens_whenValidRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        LoginResponse response = new LoginResponse("access-token", "refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh should return 200 and new access token when refresh token is valid")
    void refresh_shouldReturnNewAccessToken_whenValidRequest() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        RefreshTokenResponse response = new RefreshTokenResponse("new-access-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 400 when email or password is missing")
    void login_shouldReturnBadRequest_whenInvalidInput() throws Exception {
        // Arrange: empty email
        LoginRequest request = new LoginRequest("", "");

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 when AuthException is thrown")
    void login_shouldReturnUnauthorized_whenAuthException() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
