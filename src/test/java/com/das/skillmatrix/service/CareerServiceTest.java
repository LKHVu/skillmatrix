package com.das.skillmatrix.service;

import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.CareerDetailResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CareerService careerService;

    private CareerRequest req(String name, String desc) {
        CareerRequest r = new CareerRequest();
        r.setName(name);
        r.setDescription(desc);
        return r;
    }

    private Career career(Long id, String name, GeneralStatus status) {
        Career c = new Career();
        c.setCareerId(id);
        c.setName(name);
        c.setStatus(status);
        return c;
    }

    @Test
    @DisplayName("create() should create new when name not exists")
    void create_shouldCreateNew() {
        when(careerRepository.existsByNameIgnoreCase(eq("IT"))).thenReturn(false);

        Career saved = career(1L, "IT", GeneralStatus.ACTIVE);
        saved.setDescription("Desc");
        when(careerRepository.save(any(Career.class))).thenReturn(saved);

        CareerResponse res = careerService.create(req(" IT ", "Desc"));

        assertEquals(1L, res.getCareerId());
        assertEquals("IT", res.getName());
        assertEquals("Desc", res.getDescription());
        assertEquals(GeneralStatus.ACTIVE, res.getStatus());
        verify(careerRepository).save(any(Career.class));
    }

    @Test
    @DisplayName("create() should throw when name exists")
    void create_shouldThrow_whenExists() {
        when(careerRepository.existsByNameIgnoreCase(eq("IT"))).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> careerService.create(req("IT", "x"))
        );
        assertEquals("CAREER_NAME_EXISTS", ex.getMessage());
        verify(careerRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() should throw when career is not ACTIVE")
    void update_shouldThrow_whenNotActive() {
        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> careerService.update(1L, req("X", "d"))
        );
        assertEquals("CAREER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("update() should throw when rename to existing name")
    void update_shouldThrow_whenRenameToExisting() {
        Career current = career(1L, "IT", GeneralStatus.ACTIVE);

        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE))
                .thenReturn(Optional.of(current));
        when(careerRepository.existsByNameIgnoreCase(eq("BANKING"))).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> careerService.update(1L, req("BANKING", "d"))
        );
        assertEquals("CAREER_NAME_EXISTS", ex.getMessage());
        verify(careerRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete() should set status=DEACTIVE when career has departments")
    void delete_shouldSetDeactive_whenHasDepartments() {
        Career c = career(1L, "IT", GeneralStatus.ACTIVE);

        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE))
                .thenReturn(Optional.of(c));
        when(departmentRepository.countByCareer_CareerId(1L)).thenReturn(2L);

        careerService.delete(1L);

        assertEquals(GeneralStatus.DEACTIVE, c.getStatus());
        assertNotNull(c.getDeActiveAt());
        assertNull(c.getDeletedAt());
        verify(careerRepository).save(c);
    }

    @Test
    @DisplayName("delete() should set status=DELETED when career has no departments")
    void delete_shouldSetDeleted_whenNoDepartments() {
        Career c = career(1L, "IT", GeneralStatus.ACTIVE);

        when(careerRepository.findByCareerIdAndStatus(1L, GeneralStatus.ACTIVE))
                .thenReturn(Optional.of(c));
        when(departmentRepository.countByCareer_CareerId(1L)).thenReturn(0L);

        careerService.delete(1L);

        assertEquals(GeneralStatus.DELETED, c.getStatus());
        assertNotNull(c.getDeletedAt());
        assertNull(c.getDeActiveAt());
        verify(careerRepository).save(c);
    }

    @Test
    @DisplayName("detail() should allow ACTIVE/DEACTIVE (not DELETED)")
    void detail_shouldReturnCareerDetail_whenVisible() {
        Career c = career(1L, "IT", GeneralStatus.DEACTIVE);
        c.setDescription("Desc");

        when(careerRepository.findByCareerIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(Optional.of(c));

        Department d1 = new Department();
        d1.setDepartmentId(10L);
        d1.setName("Dev");
        Department d2 = new Department();
        d2.setDepartmentId(11L);
        d2.setName("QA");

        when(departmentRepository.findByCareer_CareerId(1L)).thenReturn(List.of(d1, d2));

        CareerDetailResponse res = careerService.detail(1L);

        assertEquals(1L, res.getCareerId());
        assertEquals("IT", res.getName());
        assertEquals("Desc", res.getDescription());
        assertEquals(GeneralStatus.DEACTIVE, res.getStatus());
        assertEquals(2, res.getDepartmentsCount());
        assertEquals("Dev", res.getDepartments().get(0).getName());
    }
}