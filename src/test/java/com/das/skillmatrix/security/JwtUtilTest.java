package com.das.skillmatrix.security;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz123456"; // must be 32+ chars

    @BeforeEach
    void setUp() {
        // expiration times: 1 hour and 7 days (in ms)
        jwtUtil = new JwtUtil(SECRET, 3600000L, 604800000L);
    }

    @Test
    @DisplayName("generateAccessToken() should create a valid JWT with correct claims")
    void generateAccessToken_shouldCreateValidJwt() {
        String token = jwtUtil.generateAccessToken("user@example.com", "USER");

        assertNotNull(token);
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertTrue(jwtUtil.validateAccessToken(token, "user@example.com"));
    }

    @Test
    @DisplayName("generateRefreshToken() should create a valid refresh JWT")
    void generateRefreshToken_shouldWork() {
        String token = jwtUtil.generateRefreshToken("user@example.com");

        assertNotNull(token);
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
        assertTrue(jwtUtil.validateRefreshToken(token));
    }

    @Test
    @DisplayName("validateAccessToken() should return false for invalid token")
    void validateAccessToken_shouldFailForInvalid() {
        assertFalse(jwtUtil.validateAccessToken("invalid.token.here", "user@example.com"));
    }

    @Test
    @DisplayName("extractEmail() should throw for malformed token")
    void extractEmail_shouldThrowForInvalid() {
        assertThrows(MalformedJwtException.class, () -> jwtUtil.extractEmail("not-a-real-jwt"));
    }
}
