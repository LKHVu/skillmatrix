package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
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

import com.das.skillmatrix.dto.request.DepartmentFilterRequest;
import com.das.skillmatrix.dto.request.DepartmentRequest;
import com.das.skillmatrix.dto.response.DepartmentDetailResponse;
import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private BusinessChangeLogService businessChangeLogService;

    @InjectMocks
    private DepartmentService departmentService;

    private DepartmentRequest req(String name, String desc, Long careerId) {
        DepartmentRequest r = new DepartmentRequest();
        r.setName(name);
        r.setDescription(desc);
        r.setCareerId(careerId);
        return r;
    }

    private Career career(Long id, GeneralStatus status) {
        Career c = new Career();
        c.setCareerId(id);
        c.setName("Career " + id);
        c.setStatus(status);
        c.setManagers(new ArrayList<>());
        return c;
    }

    private Department department(Long id, String name, GeneralStatus status, Career career) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setName(name);
        d.setDescription("Desc");
        d.setStatus(status);
        d.setCareer(career);
        d.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        d.setManagers(new ArrayList<>());
        return d;
    }

    private User user(Long id, String role, GeneralStatus status) {
        User u = new User();
        u.setUserId(id);
        u.setFullName("User " + id);
        u.setEmail("user" + id + "@test.com");
        u.setRole(role);
        u.setStatus(status);
        return u;
    }

    // ===================== CREATE =====================

    @Test
    @DisplayName("create() should create new when valid")
    void create_shouldCreateNew() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE)).thenReturn(Optional.of(c));
        when(departmentRepository.existsByNameIgnoreCaseAndCareer_CareerId("Dev", 1L)).thenReturn(false);

        Department saved = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        DepartmentResponse res = departmentService.create(req("Dev", "Desc", 1L));

        assertEquals(10L, res.getDepartmentId());
        assertEquals("Dev", res.getName());
        assertEquals("Career 1", res.getCareerName());
        assertEquals(GeneralStatus.ACTIVE, res.getStatus());
        assertNotNull(res.getCreatedAt());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("create() should throw when duplicate name in career")
    void create_shouldThrow_whenDuplicateName() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE)).thenReturn(Optional.of(c));
        when(departmentRepository.existsByNameIgnoreCaseAndCareer_CareerId("Dev", 1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> departmentService.create(req("Dev", "Desc", 1L)));
        assertEquals("DEPARTMENT_NAME_EXISTS_IN_CAREER", ex.getMessage());
    }

    // ===================== UPDATE =====================

    @Test
    @DisplayName("update() should throw when not active")
    void update_shouldThrow_whenNotActive() {
        Department d = department(10L, "Dev", GeneralStatus.DEACTIVE, career(1L, GeneralStatus.ACTIVE));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> departmentService.update(10L, req("New", "Desc", 1L)));
        assertEquals("DEPARTMENT_NOT_ACTIVE", ex.getMessage());
    }

    @Test
    @DisplayName("update() should check permission when moving career")
    void update_shouldCheckPermission_whenMovingCareer() {
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, career(1L, GeneralStatus.ACTIVE));
        Career newCareer = career(2L, GeneralStatus.ACTIVE);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(permissionService.isManagerDepartmentOnly()).thenReturn(false);
        when(careerRepository.findByCareerIdAndStatus(2L, GeneralStatus.ACTIVE)).thenReturn(Optional.of(newCareer));
        when(permissionService.canMoveDepartment(1L, 2L)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> departmentService.update(10L, req("Dev", "Desc", 2L)));
    }

    @Test
    @DisplayName("update() should block Manager Department from changing career")
    void update_shouldBlockManagerDepartment_whenChangingCareer() {
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, career(1L, GeneralStatus.ACTIVE));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(permissionService.isManagerDepartmentOnly()).thenReturn(true);

        org.springframework.security.access.AccessDeniedException ex = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> departmentService.update(10L, req("Dev", "Desc", 2L)));
        assertEquals("MANAGER_DEPARTMENT_CANNOT_CHANGE_CAREER", ex.getMessage());
    }

    @Test
    @DisplayName("update() should return response with careerName and createdAt")
    void update_shouldReturnUpdatedResponse() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(departmentRepository.save(any(Department.class))).thenReturn(d);

        DepartmentResponse res = departmentService.update(10L, req("Dev", "Updated Desc", 1L));

        assertEquals("Career 1", res.getCareerName());
        assertNotNull(res.getCreatedAt());
    }

    // ===================== DELETE =====================

    @Test
    @DisplayName("delete() should set DEACTIVE when having teams")
    void delete_shouldSetDeactive_whenHavingTeams() {
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, career(1L, GeneralStatus.ACTIVE));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(teamRepository.countByDepartment_DepartmentId(10L)).thenReturn(5L);

        departmentService.delete(10L);

        assertEquals(GeneralStatus.DEACTIVE, d.getStatus());
        assertNotNull(d.getDeActiveAt());
        verify(departmentRepository).save(d);
    }

    @Test
    @DisplayName("delete() should set DELETED when no teams")
    void delete_shouldSetDeleted_whenNoTeams() {
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, career(1L, GeneralStatus.ACTIVE));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(teamRepository.countByDepartment_DepartmentId(10L)).thenReturn(0L);

        departmentService.delete(10L);

        assertEquals(GeneralStatus.DELETED, d.getStatus());
        assertNotNull(d.getDeletedAt());
        verify(departmentRepository).save(d);
    }

    // ===================== DETAIL =====================

    @Test
    @DisplayName("detail() should return details with careerName and createdAt")
    void detail_shouldReturnDetail() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        Department d = department(10L, "Dev", GeneralStatus.DEACTIVE, c);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(teamRepository.countByDepartment_DepartmentIdAndStatusIn(10L,
                List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE))).thenReturn(3L);

        DepartmentDetailResponse res = departmentService.detail(10L);

        assertEquals(10L, res.getDepartmentId());
        assertEquals("Dev", res.getName());
        assertEquals("Career 1", res.getCareerName());
        assertNotNull(res.getCreatedAt());
        assertEquals(3L, res.getTotalTeams());
    }

    @Test
    @DisplayName("detail() should throw when DELETED")
    void detail_shouldThrow_whenDeleted() {
        Department d = department(10L, "Dev", GeneralStatus.DELETED, career(1L, GeneralStatus.ACTIVE));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> departmentService.detail(10L));
        assertEquals("DEPARTMENT_NOT_ACTIVE", ex.getMessage());
    }

    // ===================== LIST (by Career) =====================

    @Test
    @DisplayName("list() should return filtered list by career")
    @SuppressWarnings("unchecked")
    void list_shouldReturnFilteredList() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(c));

        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        Page<Department> page = new PageImpl<>(List.of(d));
        when(departmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        DepartmentFilterRequest filter = new DepartmentFilterRequest();
        PageResponse<DepartmentResponse> res = departmentService.list(1L, filter, PageRequest.of(0, 10));

        assertEquals(1, res.getTotalElements());
        assertEquals("Dev", res.getItems().get(0).getName());
        assertEquals("Career 1", res.getItems().get(0).getCareerName());
    }

    // ===================== ADD MANAGER =====================

    @Test
    @DisplayName("addManager() should add when valid")
    void addManager_shouldAddWhenValid() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        User u = user(100L, "MANAGER_DEPARTMENT", GeneralStatus.ACTIVE);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(100L)).thenReturn(Optional.of(u));
        when(departmentRepository.save(any(Department.class))).thenReturn(d);

        departmentService.addManager(10L, 100L);

        assertTrue(d.getManagers().contains(u));
        verify(departmentRepository).save(d);
    }

    @Test
    @DisplayName("addManager() should throw when invalid role")
    void addManager_shouldThrowWhenInvalidRole() {
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, career(1L, GeneralStatus.ACTIVE));
        User u = user(100L, "Manager Career", GeneralStatus.ACTIVE);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(100L)).thenReturn(Optional.of(u));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> departmentService.addManager(10L, 100L));
        assertEquals("INVALID_MANAGER_ROLE", ex.getMessage());
    }

    @Test
    @DisplayName("addManager() should skip when already manager (idempotent)")
    void addManager_shouldSkipWhenAlreadyManager() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        User u = user(100L, "MANAGER_DEPARTMENT", GeneralStatus.ACTIVE);
        d.getManagers().add(u);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(userRepository.findById(100L)).thenReturn(Optional.of(u));

        departmentService.addManager(10L, 100L);

        verify(departmentRepository, never()).save(any(Department.class));
    }

    // ===================== REMOVE MANAGER =====================

    @Test
    @DisplayName("removeManager() should remove manager (like Career pattern)")
    void removeManager_shouldRemoveManager() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        Department d = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        User u = user(100L, "Manager Department", GeneralStatus.ACTIVE);
        d.getManagers().add(u);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(departmentRepository.save(any(Department.class))).thenReturn(d);

        departmentService.removeManager(10L, 100L);

        assertFalse(d.getManagers().contains(u));
        verify(departmentRepository).save(d);
    }
}
