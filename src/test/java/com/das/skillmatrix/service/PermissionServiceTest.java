package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private PermissionService permissionService;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("ADMIN");

        normalUser = new User();
        normalUser.setUserId(2L);
        normalUser.setEmail("user@example.com");
        normalUser.setRole("USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "password", java.util.Collections.emptyList()));
    }

    @Test
    @DisplayName("checkCareerAccess() should return true for ADMIN")
    void checkCareerAccess_ShouldReturnTrueForAdmin() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findUserByEmail(adminUser.getEmail())).thenReturn(adminUser);

        assertTrue(permissionService.checkCareerAccess(1L));
        verify(careerRepository, never()).existsByCareerIdAndManagers_UserId(any(), any());
    }

    @Test
    @DisplayName("checkCareerAccess() should return true when user is career manager")
    void checkCareerAccess_ShouldReturnTrueWhenCareerManager() {
        mockSecurityContext(normalUser.getEmail());
        when(userRepository.findUserByEmail(normalUser.getEmail())).thenReturn(normalUser);
        when(careerRepository.existsByCareerIdAndManagers_UserId(10L, 2L)).thenReturn(true);

        assertTrue(permissionService.checkCareerAccess(10L));
    }

    @Test
    @DisplayName("checkDepartmentAccess() should return true when user is department manager")
    void checkDepartmentAccess_ShouldReturnTrueWhenDepartmentManager() {
        mockSecurityContext(normalUser.getEmail());
        when(userRepository.findUserByEmail(normalUser.getEmail())).thenReturn(normalUser);
        when(departmentRepository.findCareerIdByDepartmentId(20L)).thenReturn(Optional.of(10L));
        when(careerRepository.existsByCareerIdAndManagers_UserId(10L, 2L)).thenReturn(false);
        when(departmentRepository.existsByDepartmentIdAndManagers_UserId(20L, 2L)).thenReturn(true);

        assertTrue(permissionService.checkDepartmentAccess(20L));
    }

    @Test
    @DisplayName("checkTeamAccess() should return true when user is career manager")
    void checkTeamAccess_ShouldReturnTrueWhenCareerManager() {
        mockSecurityContext(normalUser.getEmail());
        when(userRepository.findUserByEmail(normalUser.getEmail())).thenReturn(normalUser);
        when(teamRepository.findCareerIdByTeamId(30L)).thenReturn(Optional.of(10L));
        when(careerRepository.existsByCareerIdAndManagers_UserId(10L, 2L)).thenReturn(true);

        assertTrue(permissionService.checkTeamAccess(30L));
        verify(departmentRepository, never()).existsByDepartmentIdAndManagers_UserId(any(), any());
        verify(teamRepository, never()).existsByTeamIdAndManagers_UserId(any(), any());
    }

    @Test
    @DisplayName("checkTeamAccess() should return true when user is team manager")
    void checkTeamAccess_ShouldReturnTrueWhenTeamManager() {
        mockSecurityContext(normalUser.getEmail());
        when(userRepository.findUserByEmail(normalUser.getEmail())).thenReturn(normalUser);
        when(teamRepository.findCareerIdByTeamId(30L)).thenReturn(Optional.of(10L));
        when(careerRepository.existsByCareerIdAndManagers_UserId(10L, 2L)).thenReturn(false);
        when(teamRepository.findDepartmentIdByTeamId(30L)).thenReturn(Optional.of(20L));
        when(departmentRepository.existsByDepartmentIdAndManagers_UserId(20L, 2L)).thenReturn(false);
        when(teamRepository.existsByTeamIdAndManagers_UserId(30L, 2L)).thenReturn(true);

        assertTrue(permissionService.checkTeamAccess(30L));
    }
}
