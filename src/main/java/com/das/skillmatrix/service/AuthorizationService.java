package com.das.skillmatrix.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.das.skillmatrix.entity.Notification;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.UpskillDocument;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.entity.UserSkillEvaluation;
import com.das.skillmatrix.entity.UserUpskillProgress;
import com.das.skillmatrix.repository.NotificationRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UpskillDocumentRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.repository.UserSkillEvaluationRepository;
import com.das.skillmatrix.repository.UserUpskillProgressRepository;

import lombok.RequiredArgsConstructor;

@Component("auth")
@RequiredArgsConstructor
public class AuthorizationService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final UserUpskillProgressRepository userUpskillProgressRepository;
    private final UpskillDocumentRepository upskillDocumentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserSkillEvaluationRepository userSkillEvaluationRepository;

    // Ownership-based
    // User
    public boolean isOwner(Long userId){
        if(isAdmin()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        return user != null && user.getUserId().equals(userId);
    }
    
    public boolean requireOwner(Long userId){
        if (isOwner(userId)) return true;
        throw new AccessDeniedException("Forbidden");
    }
    // Notification
    public boolean isNotificationOwner(Long notificationId){
        if (isAdmin()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        // if notification not found, return false
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (user == null) return false;
        if (notification == null || notification.getUser() == null) return false;
        return notification.getUser().getUserId().equals(user.getUserId());
    }

    public boolean requireNotificationOwner(Long notificationId){
        if (isNotificationOwner(notificationId)) return true;
        throw new AccessDeniedException("Forbidden");
    }
    // UserUpskillProgress
    public boolean isUserUpskillProgressOwner(Long userUpskillProgressId){
        if (isAdmin()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        // if user or progress not found, return false
        UserUpskillProgress progress = userUpskillProgressRepository.findById(userUpskillProgressId).orElse(null);
        if (user == null) return false;
        if (progress == null || progress.getUser() == null) return false;
        return progress.getUser().getUserId().equals(user.getUserId());
    }

    public boolean requireUserUpskillProgressOwner(Long userUpskillProgressId){
        if (isUserUpskillProgressOwner(userUpskillProgressId)) return true;
        throw new AccessDeniedException("Forbidden");
    }
    // UpskillDocument
    public boolean isUpskillDocumentOwner(Long documentId){
        if (isAdmin()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        // if user or document not found, return false
        UpskillDocument document = upskillDocumentRepository.findById(documentId).orElse(null);
        if (user == null) return false;
        if (document == null || document.getUploadedBy() == null) return false;
        return document.getUploadedBy().getUserId().equals(user.getUserId());
    }

    public boolean requireUpskillDocumentOwner(Long documentId){
        if (isUpskillDocumentOwner(documentId)) return true;
        throw new AccessDeniedException("Forbidden");
    }
    // Team (Manager)
    public boolean isTeamManagerOwner(Long teamId){
        if (isAdmin() || isManager()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        // if user or team not found, return false
        Team team = teamRepository.findById(teamId).orElse(null);
        if (user == null) return false;
        if (team == null || team.getManager() == null) return false;
        return team.getManager().getUserId().equals(user.getUserId());
    }

    public boolean requireTeamManagerOwner(Long teamId){
        if (isTeamManagerOwner(teamId)) return true;
        throw new AccessDeniedException("Forbidden");
    }

    // Resource-based
    // TeamMember
    public boolean isTeamMemberAccess(Long teamId){
        if (isAdmin()) return true;
        if (isTeamManagerOwner(teamId)) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        // if user or team not found, return false
        Team team = teamRepository.findById(teamId).orElse(null);
        if (user == null || user.getUserId() == null) return false;
        if (team == null) return false;
        return teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(team.getTeamId(), user.getUserId());
    }

    public boolean requireTeamMemberAccess(Long teamId){
        if (isTeamMemberAccess(teamId)) return true;
        throw new AccessDeniedException("Forbidden");
    }
    // UserSkillEvaluation
    public boolean isUserSkillEvaluationAccess(Long evaluationId){
        if (isAdmin()) return true;
        Authentication authentication = getAuthenticationOrThrow();
        User user = userRepository.findUserByEmail(authentication.getName());
        // if user or evaluation not found, return false
        UserSkillEvaluation evaluation = userSkillEvaluationRepository.findById(evaluationId).orElse(null);
        if (user == null) return false;
        if (evaluation == null) return false;
        return evaluation.getUser().getUserId().equals(user.getUserId()) || evaluation.getEvaluator().getUserId().equals(user.getUserId());
    }

    public boolean requireUserSkillEvaluationAccess(Long evaluationId){
        if (isUserSkillEvaluationAccess(evaluationId)) return true;
        throw new AccessDeniedException("Forbidden");
    }
    
    // Helper methods
    private boolean isAdmin(){
        return hasRole("ADMIN");
    }

    private boolean isManager(){
        return hasRole("MANAGER");
    }

    private boolean hasRole(String role){
        Authentication authentication = getAuthenticationOrThrow();
        return authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }

    private Authentication getAuthenticationOrThrow(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || authentication instanceof AnonymousAuthenticationToken
            || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication credentials not found");
        }
        return authentication;
    }
}
