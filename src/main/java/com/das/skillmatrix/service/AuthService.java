package com.das.skillmatrix.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.annotation.LogActivity;
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

@Service
@Transactional
public class AuthService {
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public AuthService(PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    @LogActivity(action = "LOGIN", entityType = "USER")
    public LoginResponse login(LoginRequest loginRequest) throws AuthException {
        User user = findByEmail(loginRequest.getEmail());
        RefreshToken refreshTokenInDB = new RefreshToken();
        comparePassword(loginRequest.getPassword(), user.getPasswordHash());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        // Save refresh_token into DB
        refreshTokenInDB.setUser(user);
        refreshTokenInDB.setRefreshToken(refreshToken);
        refreshTokenInDB.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiration));
        this.refreshTokenRepository.save(refreshTokenInDB);

        return new LoginResponse(accessToken, refreshToken);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) throws AuthException {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        String emailUser = jwtUtil.extractEmail(refreshToken);

        User user = findByEmail(emailUser);

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());

        return new RefreshTokenResponse(newAccessToken);
    }

    private User findByEmail(String email) throws AuthException {
        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw new AuthException("ACCOUNT_NOT_FOUND");
        }
        return user;
    }

    private void comparePassword(String rawPassword, String hashedPassword) throws AuthException {
        if (!passwordEncoder.matches(rawPassword, hashedPassword)) {
            throw new AuthException("WRONG_PASSWORD");
        }
    }

    @LogActivity(action = "LOGOUT", entityType = "USER")
    public String logout(String email) throws AuthException {
        User user = findByEmail(email);
        this.refreshTokenRepository.deleteByUser(user);

        return "Logout Success";
    }
}