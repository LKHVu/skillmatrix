package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

import com.das.skillmatrix.dto.request.DepartmentRequest;
import com.das.skillmatrix.dto.response.DepartmentDetailResponse;
import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;
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
        c.setStatus(status);
        return c;
    }

    private Department department(Long id, String name, GeneralStatus status, Career career) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setName(name);
        d.setStatus(status);
        d.setCareer(career);
        return d;
    }

    @Test
    @DisplayName("create() should create new when valid")
    void create_shouldCreateNew() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE)).thenReturn(Optional.of(c));
        when(departmentRepository.existsByNameIgnoreCaseAndCareer_CareerId("Dev", 1L)).thenReturn(false);

        Department saved = department(10L, "Dev", GeneralStatus.ACTIVE, c);
        saved.setDescription("Desc");
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        DepartmentResponse res = departmentService.create(req("Dev", "Desc", 1L));

        assertEquals(10L, res.getDepartmentId());
        assertEquals("Dev", res.getName());
        assertEquals(GeneralStatus.ACTIVE, res.getStatus());
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
        when(careerRepository.findByCareerIdAndStatus(2L, GeneralStatus.ACTIVE)).thenReturn(Optional.of(newCareer));
        when(permissionService.canMoveDepartment(1L, 2L)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> departmentService.update(10L, req("Dev", "Desc", 2L)));
    }

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

    @Test
    @DisplayName("detail() should return details for Visible department")
    void detail_shouldReturnDetail() {
        Department d = department(10L, "Dev", GeneralStatus.DEACTIVE, career(1L, GeneralStatus.ACTIVE));
        Team team = new Team();
        team.setTeamId(100L);
        team.setName("T1");
        team.setStatus(GeneralStatus.ACTIVE);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(d));
        when(teamRepository.findByDepartment_DepartmentId(10L)).thenReturn(List.of(team));

        DepartmentDetailResponse res = departmentService.detail(10L);

        assertEquals(10L, res.getDepartmentId());
        assertEquals("Dev", res.getName());
        assertEquals(1, res.getTotalTeams());
        assertEquals(1, res.getTeams().size());
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

    @Test
    @DisplayName("listByCareer() should return page of responses")
    void listByCareer_shouldReturnPage() {
        Career c = career(1L, GeneralStatus.ACTIVE);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(c));

        DepartmentResponse dr = new DepartmentResponse(10L, "Dev", "Desc", 1L, GeneralStatus.ACTIVE);
        Page<DepartmentResponse> page = new PageImpl<>(List.of(dr));

        when(departmentRepository.findDepartmentResponsesByCareerId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<DepartmentResponse> res = departmentService.listByCareer(1L, PageRequest.of(0, 10));

        assertEquals(1, res.getTotalElements());
        assertEquals("Dev", res.getItems().get(0).getName());
    }
}
