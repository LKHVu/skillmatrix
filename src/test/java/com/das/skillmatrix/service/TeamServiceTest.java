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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.request.criteria.TeamSearchCriteria;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Career;
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
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private PermissionService permissionService;
    @InjectMocks
    private TeamService teamService;
    private Department activeDepartment;
    private Team activeTeam;
    @BeforeEach
    void setUp() {
        activeDepartment = createDepartment(1L, "Dept One", GeneralStatus.ACTIVE);
        activeTeam = createTeam(1L, "Team Alpha", "desc", activeDepartment);
    }

    private static Department createDepartment(Long id, String name, GeneralStatus status) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setName(name);
        d.setStatus(status);
        return d;
    }
    private static Team createTeam(Long id, String name, String desc, Department dept) {
        Team t = new Team();
        t.setTeamId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setStatus(GeneralStatus.ACTIVE);
        t.setCreatedAt(LocalDateTime.now());
        t.setDepartment(dept);
        return t;
    }
    private static User createUser(Long id, String role) {
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
    private static Career createCareer(Long id) {
        Career c = new Career();
        c.setCareerId(id);
        c.setName("Career " + id);
        return c;
    }

    @Nested
    @DisplayName("createTeam()")
    class CreateTeam {
        @Test
        @DisplayName("should create team and return TeamResponse")
        void success() {
            TeamRequest req = new TeamRequest("New Team", "desc", 1L);
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
            when(teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                    eq("New Team"), eq(1L), anyList())).thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
                Team saved = inv.getArgument(0);
                saved.setTeamId(10L);
                return saved;
            });
            TeamResponse res = teamService.createTeam(req);
            assertEquals(10L, res.getTeamId());
            assertEquals("New Team", res.getName());
            assertEquals("desc", res.getDescription());
            assertEquals(GeneralStatus.ACTIVE, res.getStatus());
            assertNotNull(res.getDepartment());
            assertEquals(1L, res.getDepartment().getDepartmentId());
            assertEquals("Dept One", res.getDepartment().getName());
            verify(teamRepository).save(any(Team.class));
        }
        @Test
        @DisplayName("should throw when name is blank")
        void blankName() {
            TeamRequest req = new TeamRequest("   ", "desc", 1L);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.createTeam(req));
            assertEquals("TEAM_NAME_REQUIRED", ex.getMessage());
            verify(teamRepository, never()).save(any());
        }
        @Test
        @DisplayName("should throw when department not found")
        void departmentNotFound() {
            TeamRequest req = new TeamRequest("Team", "desc", 99L);
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> teamService.createTeam(req));
            assertEquals("DEPARTMENT_NOT_FOUND", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when department is not ACTIVE")
        void departmentNotActive() {
            Department inactive = createDepartment(2L, "Inactive", GeneralStatus.DEACTIVE);
            TeamRequest req = new TeamRequest("Team", "desc", 2L);
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(inactive));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.createTeam(req));
            assertEquals("DEPARTMENT_NOT_ACTIVE", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when team name already exists in department")
        void duplicateName() {
            TeamRequest req = new TeamRequest("Existing", "desc", 1L);
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
            when(teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                    eq("Existing"), eq(1L), anyList())).thenReturn(true);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.createTeam(req));
            assertEquals("TEAM_NAME_EXISTS_IN_DEPARTMENT", ex.getMessage());
            verify(teamRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateTeam()")
    class UpdateTeam {
        @Test
        @DisplayName("should update team without department change")
        void successSameDepartment() {
            TeamRequest req = new TeamRequest("Updated Name", "new desc", 1L);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(teamRepository.existsByNameAndDepartmentIdExcluding(
                    eq("Updated Name"), eq(1L), eq(1L), anyList())).thenReturn(false);
            TeamResponse res = teamService.updateTeam(1L, req);
            assertEquals("Updated Name", res.getName());
            assertEquals("new desc", res.getDescription());
            assertEquals(1L, res.getDepartment().getDepartmentId());
            verify(teamRepository).save(activeTeam);
            verify(permissionService, never()).getCurrentUser();
        }
        @Test
        @DisplayName("should update team with department change")
        void successWithDepartmentChange() {
            Department newDept = createDepartment(2L, "Dept Two", GeneralStatus.ACTIVE);
            TeamRequest req = new TeamRequest("Team Alpha", "desc", 2L);
            User admin = createUser(1L, "ADMIN");
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(permissionService.getCurrentUser()).thenReturn(admin);
            when(permissionService.isTeamManagerOnly(admin)).thenReturn(false);
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
            when(permissionService.canMoveTeamDepartment(1L, 2L)).thenReturn(true);
            when(teamRepository.existsByNameAndDepartmentIdExcluding(
                    eq("Team Alpha"), eq(2L), eq(1L), anyList())).thenReturn(false);
            TeamResponse res = teamService.updateTeam(1L, req);
            assertEquals(2L, res.getDepartment().getDepartmentId());
            assertEquals("Dept Two", res.getDepartment().getName());
            verify(teamRepository).save(activeTeam);
        }
        @Test
        @DisplayName("should throw when team not found")
        void teamNotFound() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> teamService.updateTeam(99L, new TeamRequest("X", "d", 1L)));
            assertEquals("TEAM_NOT_FOUND", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when name is blank")
        void blankName() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.updateTeam(1L, new TeamRequest("  ", "d", 1L)));
            assertEquals("TEAM_NAME_REQUIRED", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when team is not ACTIVE")
        void teamNotActive() {
            activeTeam.setStatus(GeneralStatus.DEACTIVE);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.updateTeam(1L, new TeamRequest("X", "d", 1L)));
            assertEquals("TEAM_NOT_ACTIVE", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when team manager tries to change department")
        void teamManagerCannotChangeDepartment() {
            User teamManager = createUser(5L, "MANAGER_TEAM");
            TeamRequest req = new TeamRequest("Team Alpha", "desc", 2L);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(permissionService.getCurrentUser()).thenReturn(teamManager);
            when(permissionService.isTeamManagerOnly(teamManager)).thenReturn(true);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.updateTeam(1L, req));
            assertEquals("MANAGER_TEAM_CANNOT_CHANGE_DEPARTMENT", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when target department not found")
        void targetDepartmentNotFound() {
            User admin = createUser(1L, "ADMIN");
            TeamRequest req = new TeamRequest("Team Alpha", "desc", 99L);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(permissionService.getCurrentUser()).thenReturn(admin);
            when(permissionService.isTeamManagerOnly(admin)).thenReturn(false);
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> teamService.updateTeam(1L, req));
            assertEquals("DEPARTMENT_NOT_FOUND", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when target department is not ACTIVE")
        void targetDepartmentNotActive() {
            Department inactiveDept = createDepartment(2L, "Inactive", GeneralStatus.DEACTIVE);
            User admin = createUser(1L, "ADMIN");
            TeamRequest req = new TeamRequest("Team Alpha", "desc", 2L);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(permissionService.getCurrentUser()).thenReturn(admin);
            when(permissionService.isTeamManagerOnly(admin)).thenReturn(false);
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(inactiveDept));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.updateTeam(1L, req));
            assertEquals("TARGET_DEPARTMENT_NOT_ACTIVE", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when no permission to move department")
        void accessDeniedMoveDepartment() {
            Department newDept = createDepartment(2L, "Dept Two", GeneralStatus.ACTIVE);
            User manager = createUser(3L, "MANAGER_DEPARTMENT");
            TeamRequest req = new TeamRequest("Team Alpha", "desc", 2L);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(permissionService.getCurrentUser()).thenReturn(manager);
            when(permissionService.isTeamManagerOnly(manager)).thenReturn(false);
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
            when(permissionService.canMoveTeamDepartment(1L, 2L)).thenReturn(false);
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> teamService.updateTeam(1L, req));
            assertEquals("ACCESS_DENIED_TO_MOVE_DEPARTMENT", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when name exists in target department")
        void duplicateNameInTargetDepartment() {
            TeamRequest req = new TeamRequest("Duplicate", "d", 1L);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(teamRepository.existsByNameAndDepartmentIdExcluding(
                    eq("Duplicate"), eq(1L), eq(1L), anyList())).thenReturn(true);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.updateTeam(1L, req));
            assertEquals("TEAM_NAME_EXISTS_IN_DEPARTMENT", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getAllTeams()")
    class GetAllTeams {
        @Test
        @DisplayName("should return all teams for admin user")
        void adminGetsAll() {
            User admin = createUser(1L, "ADMIN");
            Pageable pageable = PageRequest.of(0, 10);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            Page<Team> page = new PageImpl<>(List.of(activeTeam), pageable, 1);
            when(permissionService.getCurrentUser()).thenReturn(admin);
            when(permissionService.isAdmin(admin)).thenReturn(true);
            when(teamRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(page);
            PageResponse<TeamResponse> res = teamService.getAllTeams(criteria, pageable);
            assertEquals(1, res.getItems().size());
            assertEquals("Team Alpha", res.getItems().get(0).getName());
            assertEquals(1, res.getTotalElements());
        }
        @Test
        @DisplayName("should filter by department IDs for career manager")
        void careerManagerFiltered() {
            User careerMgr = createUser(2L, "MANAGER_CAREER");
            Career career = createCareer(1L);
            careerMgr.setManagedCareers(List.of(career));
            Pageable pageable = PageRequest.of(0, 10);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            Page<Team> page = new PageImpl<>(List.of(activeTeam), pageable, 1);
            when(permissionService.getCurrentUser()).thenReturn(careerMgr);
            when(permissionService.isAdmin(careerMgr)).thenReturn(false);
            when(departmentRepository.findByCareer_CareerIdIn(List.of(1L)))
                    .thenReturn(List.of(activeDepartment));
            when(teamRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(page);
            PageResponse<TeamResponse> res = teamService.getAllTeams(criteria, pageable);
            assertEquals(1, res.getItems().size());
        }
        @Test
        @DisplayName("should return empty page when user has no managed scope")
        void noManagedScope() {
            User employee = createUser(3L, "EMPLOYEE");
            Pageable pageable = PageRequest.of(0, 10);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            when(permissionService.getCurrentUser()).thenReturn(employee);
            when(permissionService.isAdmin(employee)).thenReturn(false);
            PageResponse<TeamResponse> res = teamService.getAllTeams(criteria, pageable);
            assertTrue(res.getItems().isEmpty());
            assertEquals(0, res.getTotalElements());
            verify(teamRepository, never()).findAll(any(Predicate.class), any(Pageable.class));
        }
        @Test
        @DisplayName("should filter for department manager")
        void departmentManagerFiltered() {
            User deptMgr = createUser(4L, "MANAGER_DEPARTMENT");
            Career career = createCareer(1L);
            Department dept = createDepartment(1L, "Dept", GeneralStatus.ACTIVE);
            dept.setCareer(career);
            deptMgr.setManagedDepartments(List.of(dept));
            Pageable pageable = PageRequest.of(0, 10);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            Page<Team> page = new PageImpl<>(List.of(activeTeam), pageable, 1);
            when(permissionService.getCurrentUser()).thenReturn(deptMgr);
            when(permissionService.isAdmin(deptMgr)).thenReturn(false);
            when(departmentRepository.findByCareer_CareerIdIn(List.of(1L)))
                    .thenReturn(List.of(activeDepartment));
            when(teamRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(page);
            PageResponse<TeamResponse> res = teamService.getAllTeams(criteria, pageable);
            assertEquals(1, res.getItems().size());
        }
        @Test
        @DisplayName("should filter for team manager")
        void teamManagerFiltered() {
            User teamMgr = createUser(5L, "MANAGER_TEAM");
            Team managedTeam = createTeam(10L, "Managed", "d", activeDepartment);
            teamMgr.setManagedTeams(List.of(managedTeam));
            Pageable pageable = PageRequest.of(0, 10);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            Page<Team> page = new PageImpl<>(List.of(activeTeam), pageable, 1);
            when(permissionService.getCurrentUser()).thenReturn(teamMgr);
            when(permissionService.isAdmin(teamMgr)).thenReturn(false);
            when(teamRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(page);
            PageResponse<TeamResponse> res = teamService.getAllTeams(criteria, pageable);
            assertEquals(1, res.getItems().size());
        }
    }

    @Nested
    @DisplayName("getTeamsByDepartment()")
    class GetTeamsByDepartment {
        @Test
        @DisplayName("should return teams for given department")
        void success() {
            Pageable pageable = PageRequest.of(0, 10);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            criteria.setDepartmentId(1L);
            Page<Team> page = new PageImpl<>(List.of(activeTeam), pageable, 1);
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));
            when(teamRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(page);
            PageResponse<TeamResponse> res = teamService.getTeamsByDepartment(criteria, pageable);
            assertEquals(1, res.getItems().size());
            assertEquals("Team Alpha", res.getItems().get(0).getName());
        }
        @Test
        @DisplayName("should throw when department not found")
        void departmentNotFound() {
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            criteria.setDepartmentId(99L);
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> teamService.getTeamsByDepartment(criteria, PageRequest.of(0, 10)));
        }
        @Test
        @DisplayName("should throw when department is DELETED")
        void departmentDeleted() {
            Department deleted = createDepartment(3L, "Deleted", GeneralStatus.DELETED);
            TeamSearchCriteria criteria = new TeamSearchCriteria();
            criteria.setDepartmentId(3L);
            when(departmentRepository.findById(3L)).thenReturn(Optional.of(deleted));
            assertThrows(ResourceNotFoundException.class,
                    () -> teamService.getTeamsByDepartment(criteria, PageRequest.of(0, 10)));
        }
    }

    @Nested
    @DisplayName("getTeamById()")
    class GetTeamById {
        @Test
        @DisplayName("should return TeamDetailResponse when found")
        void success() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(teamMemberRepository.countByTeam_TeamId(1L)).thenReturn(5L);
            TeamDetailResponse res = teamService.getTeamById(1L);
            assertEquals(1L, res.getTeamId());
            assertEquals("Team Alpha", res.getName());
            assertEquals("desc", res.getDescription());
            assertEquals(GeneralStatus.ACTIVE, res.getStatus());
            assertEquals(5L, res.getMemberCount());
            assertEquals(1L, res.getDepartment().getDepartmentId());
            assertEquals("Dept One", res.getDepartment().getName());
        }
        @Test
        @DisplayName("should throw when team not found")
        void notFound() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> teamService.getTeamById(99L));
            assertEquals("TEAM_NOT_FOUND", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when team is DELETED")
        void deletedTeam() {
            activeTeam.setStatus(GeneralStatus.DELETED);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> teamService.getTeamById(1L));
            assertEquals("TEAM_NOT_FOUND", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("deleteTeam()")
    class DeleteTeam {
        @Test
        @DisplayName("should deactivate team when it has members")
        void deactivateWithMembers() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(teamMemberRepository.existsByTeam_TeamId(1L)).thenReturn(true);
            String result = teamService.deleteTeam(1L);
            assertEquals("Team has been deactivated successfully.", result);
            assertEquals(GeneralStatus.DEACTIVE, activeTeam.getStatus());
            assertNotNull(activeTeam.getDeActiveAt());
            verify(teamRepository).save(activeTeam);
        }
        @Test
        @DisplayName("should soft-delete team when it has no members")
        void softDeleteWithoutMembers() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(teamMemberRepository.existsByTeam_TeamId(1L)).thenReturn(false);
            String result = teamService.deleteTeam(1L);
            assertEquals("Team deleted successfully.", result);
            assertEquals(GeneralStatus.DELETED, activeTeam.getStatus());
            assertNotNull(activeTeam.getDeletedAt());
            verify(teamRepository).save(activeTeam);
        }
        @Test
        @DisplayName("should throw when team not found")
        void notFound() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> teamService.deleteTeam(99L));
            assertEquals("TEAM_NOT_FOUND", ex.getMessage());
        }
        @Test
        @DisplayName("should throw when team is not ACTIVE")
        void notActive() {
            activeTeam.setStatus(GeneralStatus.DEACTIVE);
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.deleteTeam(1L));
            assertEquals("TEAM_NOT_ACTIVE", ex.getMessage());
            verify(teamRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignManagers()")
    class AssignManagers {
        @Test
        @DisplayName("should assign managers to team")
        void success() {
            User mgr1 = createUser(10L, "MANAGER_TEAM");
            User mgr2 = createUser(11L, "MANAGER_TEAM");
            when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
            when(userRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(mgr1, mgr2));
            teamService.assignManagers(1L, List.of(10L, 11L));
            assertEquals(2, activeTeam.getManagers().size());
            verify(teamRepository).save(activeTeam);
        }
        @Test
        @DisplayName("should throw when team not found")
        void teamNotFound() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> teamService.assignManagers(99L, List.of(1L)));
            assertEquals("TEAM_NOT_FOUND", ex.getMessage());
        }
    }
}