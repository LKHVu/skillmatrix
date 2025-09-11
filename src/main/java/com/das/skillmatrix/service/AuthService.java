package com.das.skillmatrix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.RefreshTokenRequest;
import com.das.skillmatrix.dto.response.LoginResponse;
import com.das.skillmatrix.dto.response.RefreshTokenResponse;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.security.JwtUtil;

import jakarta.security.auth.message.AuthException;

@Service
@Transactional
public class AuthService {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest loginRequest) throws AuthException {
        try {

            User user = findByEmail(loginRequest.getEmail());

            comparePassword(loginRequest.getPassword(), user.getPasswordHash());

            String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());

            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            return new LoginResponse(accessToken, refreshToken);

        } catch (Exception e) {
            throw e;
        }
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) throws AuthException {
        try {

            String refreshToken = request.getRefreshToken();

            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                throw new AuthException("Invalid or expired refresh token");
            }

            String emailUser = jwtUtil.extractEmail(refreshToken);

            User user = findByEmail(emailUser);

            String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());

            return new RefreshTokenResponse(newAccessToken);

        } catch (Exception e) {
            throw e;
        }
    }

    private User findByEmail(String email) throws AuthException {
        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw new AuthException("Invalid email or password");
        }
        return user;
    }

    private void comparePassword(String rawPassword, String hashedPassword) throws AuthException {
        if (!passwordEncoder.matches(rawPassword, hashedPassword)) {
            throw new AuthException("Invalid email or password");
        }
    }

}
