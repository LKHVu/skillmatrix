package com.das.skillmatrix.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component("auth")
@RequiredArgsConstructor
public class AuthorizationService {
    private final UserRepository userRepository;

    // Ownership-based
    public boolean isOwner(Long userId){
        if(isAdmin()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        return user != null && user.getUserId().equals(userId);
    }
    
    public boolean requiredOwner(Long userId){
        if (isOwner(userId)) return true;
        throw new AccessDeniedException("Forbidden");
    }

    // Resource-based


    // Helper methods
    private boolean isAdmin(){
        return hasRole("ADMIN");
    }

    private boolean hasRole(String role){
        Authentication authentication = getAuthenticationOrThrow();
        return authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }

    private Authentication getAuthenticationOrThrow(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication credentials not found");
        }
        return authentication;
    }
}
