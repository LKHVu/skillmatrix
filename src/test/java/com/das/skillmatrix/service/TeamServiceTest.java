package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.request.criteria.TeamSearchCriteria;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.querydsl.core.types.Predicate;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PermissionService permissionService;
    @Mock private BusinessChangeLogService businessChangeLogService;

    @InjectMocks
    private TeamService teamService;

    private Department activeDepartment;
    private Team activeTeam;

    @BeforeEach
    void setUp() {
        activeDepartment = department(1L, "Dept One", GeneralStatus.ACTIVE);
        activeTeam = team(1L, "Team Alpha", "desc", activeDepartment);
    }

    private static Department department(Long id, String name, GeneralStatus status) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setName(name);
        d.setStatus(status);
        return d;
    }

    private static Team team(Long id, String name, String desc, Department dept) {
        Team t = new Team();
        t.setTeamId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setStatus(GeneralStatus.ACTIVE);
        t.setCreatedAt(LocalDateTime.now());
        t.setDepartment(dept);
        return t;
    }

    private static User user(Long id, String role) {
        User u = new User();
        u.setUserId(id);
        u.setEmail("user" + id + "@example.com");
        u.setFullName("User " + id);
        u.setRole(role);
        u.setManagedCareers(new ArrayList<>());
        u.setManagedDepartments(new ArrayList<>());
        u.setManagedTeams(new ArrayList<>());
        return u;
    }

    @Test
    @DisplayName("createTeam() should create and return TeamResponse")
    void createTeam_success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
        when(teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                eq("New Team"), eq(1L), anyList())).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team saved = inv.getArgument(0);
            saved.setTeamId(10L);
            return saved;
        });

        TeamResponse res = teamService.createTeam(new TeamRequest("New Team", "desc", 1L));

        assertEquals(10L, res.getTeamId());
        assertEquals("New Team", res.getName());
        assertEquals(1L, res.getDepartment().getDepartmentId());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("createTeam() should throw when duplicate name in department")
    void createTeam_duplicateName() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
        when(teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                eq("Existing"), eq(1L), anyList())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(new TeamRequest("Existing", "desc", 1L)));
        assertEquals("TEAM_NAME_EXISTS_IN_DEPARTMENT", ex.getMessage());
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTeam() should update without department change")
    void updateTeam_sameDepartment() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamRepository.existsByNameAndDepartmentIdExcluding(
                eq("Updated"), eq(1L), eq(1L), anyList())).thenReturn(false);

        TeamResponse res = teamService.updateTeam(1L, new TeamRequest("Updated", "new desc", 1L));

        assertEquals("Updated", res.getName());
        assertEquals(1L, res.getDepartment().getDepartmentId());
        verify(teamRepository).save(activeTeam);
        verify(permissionService, never()).getCurrentUser();
    }

    @Test
    @DisplayName("updateTeam() should update with department change")
    void updateTeam_withDepartmentChange() {
        Department newDept = department(2L, "Dept Two", GeneralStatus.ACTIVE);
        User admin = user(1L, "ADMIN");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(permissionService.isTeamManagerOnly(admin)).thenReturn(false);
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(permissionService.canMoveTeamDepartment(1L, 2L)).thenReturn(true);
        when(teamRepository.existsByNameAndDepartmentIdExcluding(
                eq("Team Alpha"), eq(2L), eq(1L), anyList())).thenReturn(false);

        TeamResponse res = teamService.updateTeam(1L, new TeamRequest("Team Alpha", "desc", 2L));

        assertEquals(2L, res.getDepartment().getDepartmentId());
        verify(teamRepository).save(activeTeam);
    }

    @Test
    @DisplayName("getAllTeams() should return all teams for admin")
    void getAllTeams_admin() {
        User admin = user(1L, "ADMIN");
        Pageable pageable = PageRequest.of(0, 10);

        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(permissionService.isAdmin(admin)).thenReturn(true);
        when(teamRepository.findAll(any(Predicate.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(activeTeam), pageable, 1));

        PageResponse<TeamResponse> res = teamService.getAllTeams(new TeamSearchCriteria(), pageable);

        assertEquals(1, res.getItems().size());
        assertEquals("Team Alpha", res.getItems().get(0).getName());
    }

    @Test
    @DisplayName("getAllTeams() should return empty when user has no managed scope")
    void getAllTeams_emptyScope() {
        User employee = user(3L, "EMPLOYEE");
        Pageable pageable = PageRequest.of(0, 10);

        when(permissionService.getCurrentUser()).thenReturn(employee);
        when(permissionService.isAdmin(employee)).thenReturn(false);

        PageResponse<TeamResponse> res = teamService.getAllTeams(new TeamSearchCriteria(), pageable);

        assertTrue(res.getItems().isEmpty());
        assertEquals(0, res.getTotalElements());
        verify(teamRepository, never()).findAll(any(Predicate.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getTeamsByDepartment() should return teams")
    void getTeamsByDepartment_success() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamSearchCriteria criteria = new TeamSearchCriteria();
        criteria.setDepartmentId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
        when(teamRepository.findAll(any(Predicate.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(activeTeam), pageable, 1));

        PageResponse<TeamResponse> res = teamService.getTeamsByDepartment(criteria, pageable);

        assertEquals(1, res.getItems().size());
    }

    @Test
    @DisplayName("getTeamById() should return detail when found")
    void getTeamById_success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamMemberRepository.countByTeam_TeamId(1L)).thenReturn(5L);

        TeamDetailResponse res = teamService.getTeamById(1L);

        assertEquals(1L, res.getTeamId());
        assertEquals("Team Alpha", res.getName());
        assertEquals(5L, res.getMemberCount());
        assertEquals(1L, res.getDepartment().getDepartmentId());
    }

    @Test
    @DisplayName("getTeamById() should throw when not found")
    void getTeamById_notFound() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> teamService.getTeamById(99L));
        assertEquals("TEAM_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("deleteTeam() should deactivate when has members and log status change")
    void deleteTeam_deactivate() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamMemberRepository.existsByTeam_TeamId(1L)).thenReturn(true);

        String result = teamService.deleteTeam(1L);

        assertEquals("Team has been deactivated successfully.", result);
        assertEquals(GeneralStatus.DEACTIVE, activeTeam.getStatus());
        assertNotNull(activeTeam.getDeActiveAt());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("CHANGE_TEAM_STATUS"), eq("TEAM"), eq(1L),
                eq("status"), eq("ACTIVE"), eq("DEACTIVE"));
    }

    @Test
    @DisplayName("deleteTeam() should soft-delete when no members and log status change")
    void deleteTeam_softDelete() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamMemberRepository.existsByTeam_TeamId(1L)).thenReturn(false);

        String result = teamService.deleteTeam(1L);

        assertEquals("Team deleted successfully.", result);
        assertEquals(GeneralStatus.DELETED, activeTeam.getStatus());
        assertNotNull(activeTeam.getDeletedAt());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("CHANGE_TEAM_STATUS"), eq("TEAM"), eq(1L),
                eq("status"), eq("ACTIVE"), eq("DELETED"));
    }

    @Test
    @DisplayName("assignManagers() should assign and log reassignment")
    void assignManagers_success() {
        User oldMgr = user(5L, "MANAGER_TEAM");
        activeTeam.setManagers(new ArrayList<>(List.of(oldMgr)));

        User mgr1 = user(10L, "MANAGER_TEAM");
        User mgr2 = user(11L, "MANAGER_TEAM");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(userRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(mgr1, mgr2));

        teamService.assignManagers(1L, List.of(10L, 11L));

        assertEquals(2, activeTeam.getManagers().size());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("REASSIGN_TEAM_MANAGERS"), eq("TEAM"), eq(1L),
                eq("managerIds"), eq("[5]"), eq("[10, 11]"));
    }
}
