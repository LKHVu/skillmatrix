package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.das.skillmatrix.dto.request.CreateUserRequest;
import com.das.skillmatrix.dto.request.DeactivateUserRequest;
import com.das.skillmatrix.dto.request.UpdateUserRequest;
import com.das.skillmatrix.dto.request.UserFilterRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.UserDetailResponse;
import com.das.skillmatrix.dto.response.UserResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Position;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.PositionRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private CareerRepository careerRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionService permissionService;
    @Mock
    private BusinessChangeLogService logService;

    @InjectMocks
    private UserService userService;

    // ===================== HELPERS =====================

    private User user(Long id, String email, String role, GeneralStatus status) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        u.setRole(role);
        u.setStatus(status);
        u.setPositions(new ArrayList<>());
        u.setManagedCareers(new ArrayList<>());
        u.setManagedDepartments(new ArrayList<>());
        u.setManagedTeams(new ArrayList<>());
        return u;
    }

    private Position position(Long id, String name) {
        Position p = new Position();
        p.setPositionId(id);
        p.setName(name);
        p.setStatus(GeneralStatus.ACTIVE);
        return p;
    }

    private Career career(Long id) {
        Career c = new Career();
        c.setCareerId(id);
        c.setName("Career " + id);
        c.setStatus(GeneralStatus.ACTIVE);
        c.setManagers(new ArrayList<>());
        return c;
    }

    private Department department(Long id, Career career) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setName("Dept " + id);
        d.setStatus(GeneralStatus.ACTIVE);
        d.setCareer(career);
        d.setManagers(new ArrayList<>());
        return d;
    }

    private Team team(Long id, Department dept) {
        Team t = new Team();
        t.setTeamId(id);
        t.setName("Team " + id);
        t.setStatus(GeneralStatus.ACTIVE);
        t.setDepartment(dept);
        t.setManagers(new ArrayList<>());
        return t;
    }

    private CreateUserRequest createReq(String email, String role, List<Long> positionIds) {
        CreateUserRequest r = new CreateUserRequest();
        r.setEmail(email);
        r.setRole(role);
        r.setPositionIds(positionIds);
        return r;
    }

    // ===================== CREATE =====================

    @Test
    @DisplayName("create() should create user with ADMIN role (no positions required)")
    void create_shouldCreateAdmin_withoutPositions() {
        User admin = user(1L, "admin@test.com", "ADMIN", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        User saved = user(10L, "new@test.com", "ADMIN", GeneralStatus.ACTIVE);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        CreateUserRequest req = createReq("new@test.com", "ADMIN", null);
        UserResponse res = userService.create(req);

        assertEquals(10L, res.getUserId());
        assertEquals("new@test.com", res.getEmail());
        verify(userRepository).save(any(User.class));
        verify(logService).log(eq("CREATE_USER"), eq("USER"), eq(10L), anyList());
    }

    @Test
    @DisplayName("create() should throw when email already exists")
    void create_shouldThrow_whenEmailExists() {
        User admin = user(1L, "admin@test.com", "ADMIN", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(userRepository.existsByEmailIgnoreCase("dup@test.com")).thenReturn(true);

        CreateUserRequest req = createReq("dup@test.com", "STAFF", List.of(1L));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(req));
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() should throw when non-ADMIN role has no positions")
    void create_shouldThrow_whenStaffNoPositions() {
        User admin = user(1L, "admin@test.com", "ADMIN", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(userRepository.existsByEmailIgnoreCase("staff@test.com")).thenReturn(false);

        CreateUserRequest req = createReq("staff@test.com", "STAFF", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(req));
        assertEquals("POSITION_REQUIRED", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw when Manager Career tries to create Admin")
    void create_shouldThrow_whenManagerCareerCreatesAdmin() {
        User manager = user(1L, "mc@test.com", "MANAGER_CAREER", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(manager);
        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);

        CreateUserRequest req = createReq("new@test.com", "ADMIN", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(req));
        assertEquals("ROLE_EXCEEDS_PERMISSION", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw when Manager Department tries to create Manager Career")
    void create_shouldThrow_whenManagerDeptCreatesManagerCareer() {
        User manager = user(1L, "md@test.com", "MANAGER_DEPARTMENT", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(manager);
        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);

        CreateUserRequest req = createReq("new@test.com", "MANAGER_CAREER", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(req));
        assertEquals("ROLE_EXCEEDS_PERMISSION", ex.getMessage());
    }

    @Test
    @DisplayName("create() should throw when Manager Team tries to create any user")
    void create_shouldThrow_whenManagerTeamCreates() {
        User manager = user(1L, "mt@test.com", "MANAGER_TEAM", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(manager);
        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);

        CreateUserRequest req = createReq("new@test.com", "STAFF", List.of(1L));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(req));
        assertEquals("ROLE_EXCEEDS_PERMISSION", ex.getMessage());
    }

    // ===================== UPDATE =====================

    @Test
    @DisplayName("update() should update email and role")
    void update_shouldUpdateEmailAndRole() {
        User admin = user(1L, "admin@test.com", "ADMIN", GeneralStatus.ACTIVE);
        User target = user(10L, "old@test.com", "STAFF", GeneralStatus.ACTIVE);
        target.setManagedCareers(new ArrayList<>());
        target.setManagedDepartments(new ArrayList<>());
        target.setManagedTeams(new ArrayList<>());
        Position pos = position(1L, "Dev");

        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(userRepository.existsByEmailIgnoreCaseAndUserIdNot("new@test.com", 10L)).thenReturn(false);
        when(positionRepository.findByPositionIdInAndStatus(List.of(1L), GeneralStatus.ACTIVE))
                .thenReturn(List.of(pos));

        Department dept = department(1L, career(1L));
        Team team = team(1L, dept);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(1L, 10L)).thenReturn(false);

        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("new@test.com");
        req.setRole("STAFF");
        req.setPositionIds(List.of(1L));
        req.setTeamId(1L);

        when(userRepository.save(any(User.class))).thenReturn(target);

        UserResponse res = userService.update(10L, req);

        assertEquals("new@test.com", res.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("update() should throw when user not found")
    void update_shouldThrow_whenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("x@test.com");
        req.setRole("STAFF");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.update(999L, req));
        assertEquals("USER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("update() should throw when email is duplicate")
    void update_shouldThrow_whenEmailDuplicate() {
        User admin = user(1L, "admin@test.com", "ADMIN", GeneralStatus.ACTIVE);
        User target = user(10L, "old@test.com", "STAFF", GeneralStatus.ACTIVE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(userRepository.existsByEmailIgnoreCaseAndUserIdNot("dup@test.com", 10L)).thenReturn(true);

        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("dup@test.com");
        req.setRole("STAFF");
        req.setPositionIds(List.of(1L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.update(10L, req));
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getMessage());
    }

    // ===================== DEACTIVATE / DELETE =====================

    @Test
    @DisplayName("deactivateOrDelete() should deactivate with UNLIMITED")
    void deactivate_shouldSetUnlimited() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("DEACTIVE");
        req.setDeactiveType("UNLIMITED");

        userService.deactivateOrDelete(10L, req);

        assertEquals(GeneralStatus.DEACTIVE, target.getStatus());
        assertEquals("UNLIMITED", target.getDeactiveType());
        assertNull(target.getDeactiveUntil());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("deactivateOrDelete() should deactivate with TEMPORARY + duration")
    void deactivate_shouldSetTemporary() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("DEACTIVE");
        req.setDeactiveType("TEMPORARY");
        req.setDuration("7_DAYS");

        userService.deactivateOrDelete(10L, req);

        assertEquals(GeneralStatus.DEACTIVE, target.getStatus());
        assertEquals("TEMPORARY", target.getDeactiveType());
        assertNotNull(target.getDeactiveUntil());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("deactivateOrDelete() should throw when TEMPORARY but no duration")
    void deactivate_shouldThrow_whenNoDuration() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("DEACTIVE");
        req.setDeactiveType("TEMPORARY");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.deactivateOrDelete(10L, req));
        assertEquals("DEACTIVATION_DURATION_REQUIRED", ex.getMessage());
    }

    @Test
    @DisplayName("deactivateOrDelete() should delete user permanently")
    void deactivate_shouldDeletePermanently() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("DELETE");

        userService.deactivateOrDelete(10L, req);

        verify(userRepository).delete(target);
    }

    @Test
    @DisplayName("deactivateOrDelete() should throw when user already DEACTIVE")
    void deactivate_shouldThrow_whenAlreadyDeactive() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.DEACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("DEACTIVE");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.deactivateOrDelete(10L, req));
        assertEquals("CANNOT_DEACTIVE_DEACTIVED_USER", ex.getMessage());
    }

    @Test
    @DisplayName("deactivateOrDelete() should throw on invalid action")
    void deactivate_shouldThrow_whenInvalidAction() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("INVALID");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.deactivateOrDelete(10L, req));
        assertEquals("INVALID_ACTION", ex.getMessage());
    }

    // ===================== REACTIVATE =====================

    @Test
    @DisplayName("reactivate() should reactivate deactive user")
    void reactivate_shouldReactivate() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.DEACTIVE);
        target.setDeactiveType("UNLIMITED");
        target.setPositions(new ArrayList<>());
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenReturn(target);

        UserResponse res = userService.reactivate(10L);

        assertEquals(GeneralStatus.ACTIVE, target.getStatus());
        assertNull(target.getDeactiveType());
        assertNull(target.getDeactiveUntil());
        assertNull(target.getDeActiveAt());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("reactivate() should throw when already active")
    void reactivate_shouldThrow_whenAlreadyActive() {
        User target = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.reactivate(10L));
        assertEquals("USER_NOT_DEACTIVE", ex.getMessage());
    }

    // ===================== LIST =====================

    @Test
    @DisplayName("list() should return paged user list")
    @SuppressWarnings("unchecked")
    void list_shouldReturnPagedList() {
        User admin = user(1L, "admin@test.com", "ADMIN", GeneralStatus.ACTIVE);
        when(permissionService.getCurrentUser()).thenReturn(admin);

        User u = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        u.setPositions(new ArrayList<>());
        Page<User> page = new PageImpl<>(List.of(u));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        UserFilterRequest filter = new UserFilterRequest();
        PageResponse<UserResponse> res = userService.list(filter, PageRequest.of(0, 10));

        assertEquals(1, res.getTotalElements());
        assertEquals("u@test.com", res.getItems().get(0).getEmail());
    }

    @Test
    @DisplayName("listByTeam() should throw when team not found")
    void listByTeam_shouldThrow_whenTeamNotFound() {
        when(teamRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.listByTeam(999L, new UserFilterRequest(), PageRequest.of(0, 5)));
        assertEquals("TEAM_NOT_FOUND", ex.getMessage());
    }

    // ===================== DETAIL =====================

    @Test
    @DisplayName("getDetail() should return user detail")
    void getDetail_shouldReturnDetail() {
        User u = user(10L, "u@test.com", "STAFF", GeneralStatus.ACTIVE);
        u.setFullName("Test User");
        u.setPositions(new ArrayList<>());
        when(userRepository.findById(10L)).thenReturn(Optional.of(u));

        UserDetailResponse res = userService.getDetail(10L);

        assertEquals(10L, res.getUserId());
        assertEquals("u@test.com", res.getEmail());
        assertEquals("Test User", res.getFullName());
    }

    @Test
    @DisplayName("getDetail() should throw when not found")
    void getDetail_shouldThrow_whenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.das.skillmatrix.exception.ResourceNotFoundException.class,
                () -> userService.getDetail(999L));
    }
}
