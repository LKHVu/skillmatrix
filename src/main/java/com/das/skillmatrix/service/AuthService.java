package com.das.skillmatrix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.dto.request.LoginRequest;
import com.das.skillmatrix.dto.request.LogoutRequest;
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
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest loginRequest) throws AuthException {
        User user = findByEmail(loginRequest.getEmail());

        comparePassword(loginRequest.getPassword(), user.getPasswordHash());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

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
    
    public String logout(LogoutRequest request) throws AuthException{
    	String emailUser = jwtUtil.extractEmail(request.getAccessToken());
    	if (!jwtUtil.validateAccessToken(request.getAccessToken(), emailUser)) {
    		throw new AuthException();
    	}
    		return "Logout Success";
    	}
}
