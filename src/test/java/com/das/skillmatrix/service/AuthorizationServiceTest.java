package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserUpskillProgressRepository userUpskillProgressRepository;
    @Mock
    private UpskillDocumentRepository upskillDocumentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserSkillEvaluationRepository userSkillEvaluationRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsUser(String email, String role) {
        var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(Long userId, String email){
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        return user;
    }

    // User Ownership
    @Test
    @DisplayName("isOwner() should return true when user is admin")
    void isOwner_shouldReturnTrue_whenUserIsAdmin() {
        loginAsUser("admin@example.com", "ADMIN");

        assertTrue(authorizationService.isOwner(1L));
    }

    @Test
    @DisplayName("isOwner() should return true when user is self")
    void isOwner_shouldReturnTrue_whenUserIsSelf() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        
        assertTrue(authorizationService.isOwner(1L));
    }

    @Test
    @DisplayName("requireOwner() should throw 403 when user is not admin or self")
    void requireOwner_shouldThrowAccessDeniedException_whenUserIsNotAdminOrSelf() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireOwner(2L));
    }

    // Notification Ownership
    @Test
    @DisplayName("isNotificationOwner() should return true when user is owner")
    void isNotificationOwner_shouldReturnTrue_whenUserIsOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        Notification notification = new Notification();
        notification.setUser(user(1L, "user@example.com"));
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(notification));
        
        assertTrue(authorizationService.isNotificationOwner(2L));
    }

    @Test
    @DisplayName("requireNotificationOwner() should throw 403 when user is not owner")
    void requireNotificationOwner_shouldThrow403_whenNotOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        Notification notification = new Notification();
        notification.setUser(user(2L, "owner@example.com"));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> authorizationService.requireNotificationOwner(1L));
    }

    
    // UserUpskillProgress Ownership
    @Test
    @DisplayName("isUserUpskillProgressOwner() should return true when user is owner")
    void isUserUpskillProgressOwner_shouldReturnTrue_whenUserIsOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        UserUpskillProgress progress = new UserUpskillProgress();
        progress.setUser(user(1L, "user@example.com"));
        when(userUpskillProgressRepository.findById(2L)).thenReturn(Optional.of(progress));
        
        assertTrue(authorizationService.isUserUpskillProgressOwner(2L));
    }

    @Test
    @DisplayName("requireUserUpskillProgressOwner() should throw 403 when user is not owner")
    void requireUserUpskillProgressOwner_shouldThrow403_whenUserIsNotOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        UserUpskillProgress progress = new UserUpskillProgress();
        progress.setUser(user(2L, "owner@example.com"));
        when(userUpskillProgressRepository.findById(1L)).thenReturn(Optional.of(progress));

        assertThrows(AccessDeniedException.class, () -> authorizationService.requireUserUpskillProgressOwner(1L));
    }

    // UpskillDocument Ownership
    @Test
    @DisplayName("isUpskillDocumentOwner() should return true when user is owner")
    void isUpskillDocumentOwner_shouldReturnTrue_whenUserIsOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        UpskillDocument document = new UpskillDocument();
        document.setUploadedBy(user(1L, "user@example.com"));
        when(upskillDocumentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertTrue(authorizationService.isUpskillDocumentOwner(1L));
    }

    @Test
    @DisplayName("requireUpskillDocumentOwner() should throw 403 when user is not owner")
    void requireUpskillDocumentOwner_shouldThrow403_whenUserIsNotOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        UpskillDocument document = new UpskillDocument();
        document.setUploadedBy(user(2L, "owner@example.com"));
        when(upskillDocumentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThrows(AccessDeniedException.class, () -> authorizationService.requireUpskillDocumentOwner(1L));
    }

    // Team Manager
    @Test
    @DisplayName("isTeamManagerOwner() should return true when user is owner")
    void isTeamManagerOwner_shouldReturnTrue_whenUserIsOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        Team team = new Team();
        team.setManager(user(1L, "user@example.com"));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertTrue(authorizationService.isTeamManagerOwner(1L));
    }

    @Test
    @DisplayName("requireTeamManagerOwner() should throw 403 when user is not owner")
    void requireTeamManagerOwner_shouldThrow403_whenUserIsNotOwner() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        Team team = new Team();
        team.setManager(user(2L, "owner@example.com"));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertThrows(AccessDeniedException.class, () -> authorizationService.requireTeamManagerOwner(1L));
    }

    // TeamMember
    @Test
    @DisplayName("isTeamMemberAccess() should return true when user is member")
    void isTeamMemberAccess_shouldReturnTrue_whenUserIsMember() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        Team team = new Team();
        team.setTeamId(2L);
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(2L, 1L)).thenReturn(true);

        assertTrue(authorizationService.isTeamMemberAccess(2L));
    }

    @Test
    @DisplayName("requireTeamMemberAccess() should throw 403 when user is not member")
    void requireTeamMemberAccess_shouldThrow403_whenUserIsNotMember() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        Team team = new Team();
        team.setTeamId(2L);
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(2L, 1L)).thenReturn(false);
    
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireTeamMemberAccess(2L));
    }

    // UserSkillEvaluation
    @Test
    @DisplayName("isUserSkillEvaluationAccess() should return true when user is evaluator or user")
    void isUserSkillEvaluationAccess_shouldReturnTrue_whenUserIsEvaluatorOrUser() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        UserSkillEvaluation evaluation = new UserSkillEvaluation();
        evaluation.setUser(user(1L, "user@example.com"));
        evaluation.setEvaluator(user(2L, "evaluator@example.com"));
        when(userSkillEvaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertTrue(authorizationService.isUserSkillEvaluationAccess(1L));
    }

    @Test
    @DisplayName("requireUserSkillEvaluationAccess() should throw 403 when user is not evaluator or user")
    void requireUserSkillEvaluationAccess_shouldThrow403_whenUserIsNotEvaluatorOrUser() {
        loginAsUser("user@example.com", "USER");
        when(userRepository.findUserByEmail("user@example.com")).thenReturn(user(1L, "user@example.com"));
        UserSkillEvaluation evaluation = new UserSkillEvaluation();
        evaluation.setUser(user(3L, "other@example.com"));
        evaluation.setEvaluator(user(2L, "evaluator@example.com"));
        when(userSkillEvaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThrows(AccessDeniedException.class, () -> authorizationService.requireUserSkillEvaluationAccess(1L));
    }
}
