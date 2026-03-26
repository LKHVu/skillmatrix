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
import org.springframework.data.jpa.domain.Specification;

import com.das.skillmatrix.dto.request.TeamFilterRequest;
import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

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
        t.setManagers(new ArrayList<>());
        return t;
    }

    private static User user(Long id, String role) {
        User u = new User();
        u.setUserId(id);
        u.setEmail("user" + id + "@example.com");
        u.setFullName("User " + id);
        u.setRole(role);
        u.setStatus(GeneralStatus.ACTIVE);
        u.setManagedCareers(new ArrayList<>());
        u.setManagedDepartments(new ArrayList<>());
        u.setManagedTeams(new ArrayList<>());
        return u;
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("create() should create and return TeamResponse")
    void create_success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
        when(teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                eq("New Team"), eq(1L), anyList())).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team saved = inv.getArgument(0);
            saved.setTeamId(10L);
            return saved;
        });

        TeamResponse res = teamService.create(new TeamRequest("New Team", "desc", 1L));

        assertEquals(10L, res.getTeamId());
        assertEquals("New Team", res.getName());
        assertEquals(1L, res.getDepartmentId());
        assertEquals("Dept One", res.getDepartmentName());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("create() should throw when duplicate name in department")
    void create_duplicateName() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
        when(teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                eq("Existing"), eq(1L), anyList())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamService.create(new TeamRequest("Existing", "desc", 1L)));
        assertEquals("TEAM_NAME_EXISTS_IN_DEPARTMENT", ex.getMessage());
        verify(teamRepository, never()).save(any());
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("update() should update without department change")
    void update_sameDepartment() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));

        TeamResponse res = teamService.update(1L, new TeamRequest("Updated", "new desc", 1L));

        assertEquals("Updated", res.getName());
        assertEquals(1L, res.getDepartmentId());
        verify(teamRepository).save(activeTeam);
        verify(permissionService, never()).getCurrentUser();
    }

    @Test
    @DisplayName("update() should update with department change")
    void update_withDepartmentChange() {
        Department newDept = department(2L, "Dept Two", GeneralStatus.ACTIVE);
        User admin = user(1L, "ADMIN");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(permissionService.isTeamManagerOnly(admin)).thenReturn(false);
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(permissionService.canMoveTeamDepartment(1L, 2L)).thenReturn(true);

        TeamResponse res = teamService.update(1L, new TeamRequest("Team Alpha", "desc", 2L));

        assertEquals(2L, res.getDepartmentId());
        verify(teamRepository).save(activeTeam);
    }

    // ==================== LIST ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list() should return all teams for admin (global search)")
    void list_admin() {
        User admin = user(1L, "ADMIN");
        Pageable pageable = PageRequest.of(0, 10);

        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(permissionService.isAdmin(admin)).thenReturn(true);
        when(teamRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(activeTeam), pageable, 1));

        PageResponse<TeamResponse> res = teamService.list(new TeamFilterRequest(), pageable);

        assertEquals(1, res.getItems().size());
        assertEquals("Team Alpha", res.getItems().get(0).getName());
    }

    @Test
    @DisplayName("list() should return empty when user has no managed scope")
    void list_emptyScope() {
        User employee = user(3L, "EMPLOYEE");
        Pageable pageable = PageRequest.of(0, 10);

        when(permissionService.getCurrentUser()).thenReturn(employee);
        when(permissionService.isAdmin(employee)).thenReturn(false);

        PageResponse<TeamResponse> res = teamService.list(new TeamFilterRequest(), pageable);

        assertTrue(res.getItems().isEmpty());
        assertEquals(0, res.getTotalElements());
        verify(teamRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("list() with departmentId should return filtered teams")
    void list_byDepartment() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamFilterRequest filter = new TeamFilterRequest();
        filter.setDepartmentId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
        when(teamRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(activeTeam), pageable, 1));

        PageResponse<TeamResponse> res = teamService.list(filter, pageable);

        assertEquals(1, res.getItems().size());
    }

    // ==================== DETAIL ====================

    @Test
    @DisplayName("detail() should return detail when found")
    void detail_success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamMemberRepository.countByTeam_TeamId(1L)).thenReturn(5L);

        TeamDetailResponse res = teamService.detail(1L);

        assertEquals(1L, res.getTeamId());
        assertEquals("Team Alpha", res.getName());
        assertEquals(5L, res.getMemberCount());
        assertEquals(1L, res.getDepartmentId());
        assertEquals("Dept One", res.getDepartmentName());
    }

    @Test
    @DisplayName("detail() should throw when not found")
    void detail_notFound() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamService.detail(99L));
        assertEquals("TEAM_NOT_FOUND", ex.getMessage());
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("delete() should deactivate when has members and log status change")
    void delete_deactivate() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamMemberRepository.existsByTeam_TeamId(1L)).thenReturn(true);

        teamService.delete(1L);
        assertEquals(GeneralStatus.DEACTIVE, activeTeam.getStatus());
        assertNotNull(activeTeam.getDeActiveAt());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("CHANGE_TEAM_STATUS"), eq("TEAM"), eq(1L),
                eq("status"), eq("ACTIVE"), eq("DEACTIVE"));
    }

    @Test
    @DisplayName("delete() should soft-delete when no members and log status change")
    void delete_softDelete() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(teamMemberRepository.existsByTeam_TeamId(1L)).thenReturn(false);

        teamService.delete(1L);
        assertEquals(GeneralStatus.DELETED, activeTeam.getStatus());
        assertNotNull(activeTeam.getDeletedAt());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("CHANGE_TEAM_STATUS"), eq("TEAM"), eq(1L),
                eq("status"), eq("ACTIVE"), eq("DELETED"));
    }

    // ==================== ADD/REMOVE MANAGER ====================

    @Test
    @DisplayName("addManager() should add manager and log")
    void addManager_success() {
        User mgr = user(5L, "MANAGER_TEAM");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(userRepository.findById(5L)).thenReturn(Optional.of(mgr));

        teamService.addManager(1L, 5L);

        assertEquals(1, activeTeam.getManagers().size());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("ADD_TEAM_MANAGER"), eq("TEAM"), eq(1L),
                eq("managerId"), isNull(), eq("5"));
    }

    @Test
    @DisplayName("removeManager() should remove manager and log")
    void removeManager_success() {
        User oldMgr = user(5L, "Manager Team");
        activeTeam.setManagers(new ArrayList<>(List.of(oldMgr)));

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));

        teamService.removeManager(1L, 5L);

        assertEquals(0, activeTeam.getManagers().size());
        verify(teamRepository).save(activeTeam);
        verify(businessChangeLogService).log(
                eq("REMOVE_TEAM_MANAGER"), eq("TEAM"), eq(1L),
                eq("managerId"), eq("5"), isNull());
    }
}
