package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.xml.crypto.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class AuthService {
    private final long refreshExpiration;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public AuthService(@Value("${jwt.refresh.expiration}") long refreshExpiration,
    				   PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil) {
        this.refreshExpiration = refreshExpiration;
		this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest loginRequest) throws AuthException {
        User user = findByEmail(loginRequest.getEmail());
        RefreshToken refreshTokenInDB = new RefreshToken();
        comparePassword(loginRequest.getPassword(), user.getPasswordHash());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        // Save refresh_token in DB
        refreshTokenInDB.setUser(user);
        refreshTokenInDB.setRefreshToken(refreshToken);
        refreshTokenInDB.setExpiresAt(LocalDateTime.now().plus(refreshExpiration, ChronoUnit.MILLIS));
        refreshTokenInDB.setCreatedAt(LocalDateTime.now());
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
    
    public String logout(HttpServletRequest request, RefreshTokenRequest token) throws AuthException{
    	String authHeader = request.getHeader("Authorization");
    	if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    		throw new AuthException();
    	}
    	String accessToken = authHeader.substring(7);
    	if (!jwtUtil.validateAccessToken(accessToken, jwtUtil.extractEmail(accessToken))) {
    		throw new AuthException();
    	}
    	User user = findByEmail(jwtUtil.extractEmail(accessToken));
    	this.refreshTokenRepository.deleteByRefreshTokenAndUser(token.getRefreshToken(), user);
    	
    	return "Logout Success";
    }
}
