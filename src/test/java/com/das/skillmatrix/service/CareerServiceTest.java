package com.das.skillmatrix.service;

import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.CareerDeleteCheckResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.PositionRepository;

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

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private CareerService careerService;

    private CareerRequest req(String name, String desc) {
        CareerRequest r = new CareerRequest();
        r.setName(name);
        r.setDescription(desc);
        return r;
    }

    private Career career(Long id, String name, boolean deleted) {
        Career c = new Career();
        c.setCareerId(id);
        c.setName(name);
        c.setDeleted(deleted);
        return c;
    }

    @Test
    @DisplayName("create() should create new when not exists")
    void create_shouldCreateNew() {
        when(careerRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.empty());

        Career saved = career(1L, "IT", false);
        saved.setDescription("Desc");
        when(careerRepository.save(any(Career.class))).thenReturn(saved);

        var res = careerService.create(req(" IT ", "Desc"));

        assertEquals(1L, res.getCareerId());
        assertEquals("IT", res.getName());
        assertEquals(0, res.getDepartmentsCount());
    }

    @Test
    @DisplayName("create() should throw when existing is active")
    void create_shouldThrow_whenExistsActive() {
        when(careerRepository.findFirstByNameIgnoreCase("IT"))
                .thenReturn(Optional.of(career(1L, "IT", false)));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> careerService.create(req("IT", "x")));
        assertEquals("CAREER_NAME_EXISTS", ex.getMessage());
    }

    @Test
    @DisplayName("create() should undelete when existing is deleted")
    void create_shouldUndelete_whenExistsDeleted() {
        Career existing = career(1L, "IT", true);

        when(careerRepository.findFirstByNameIgnoreCase("IT"))
                .thenReturn(Optional.of(existing));

        when(departmentRepository.countByCareer_CareerId(1L)).thenReturn(2L);

        Career saved = career(1L, "IT", false);
        saved.setDescription("NewDesc");
        when(careerRepository.save(any(Career.class))).thenReturn(saved);

        var res = careerService.create(req("IT", "NewDesc"));

        assertEquals(1L, res.getCareerId());
        assertEquals("IT", res.getName());
        assertEquals(2, res.getDepartmentsCount());
        assertFalse(existing.getDeleted()); // đã được undelete
    }

    @Test
    @DisplayName("deleteCheck() should return requireConfirm=true when has deps or positions")
    void deleteCheck_shouldRequireConfirm() {
        Career c = career(1L, "IT", false);
        when(careerRepository.findByCareerIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));

        Department d = new Department();
        d.setDepartmentId(10L);
        d.setName("Dev");
        when(departmentRepository.findByCareer_CareerId(1L)).thenReturn(List.of(d));

        when(positionRepository.countByDepartment_Career_CareerId(1L)).thenReturn(5L);

        CareerDeleteCheckResponse res = careerService.deleteCheck(1L);

        assertTrue(res.isRequireConfirm());
        assertEquals(1, res.getDepartmentsCount());
        assertEquals(5, res.getPositionsCount());
        assertEquals("Dev", res.getDepartments().get(0).getName());
    }

    @Test
    @DisplayName("delete() should throw confirm required when has deps/pos and confirm=false")
    void delete_shouldThrow_whenConfirmRequired() {
        Career c = career(1L, "IT", false);
        when(careerRepository.findByCareerIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));

        when(departmentRepository.countByCareer_CareerId(1L)).thenReturn(1L);
        when(positionRepository.countByDepartment_Career_CareerId(1L)).thenReturn(0L);

        var ex = assertThrows(IllegalStateException.class,
                () -> careerService.delete(1L, false));
        assertEquals("CAREER_DELETE_CONFIRM_REQUIRED", ex.getMessage());

        verify(careerRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete() should soft delete when confirm=true")
    void delete_shouldSoftDelete_whenConfirmTrue() {
        Career c = career(1L, "IT", false);
        when(careerRepository.findByCareerIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));

        when(departmentRepository.countByCareer_CareerId(1L)).thenReturn(1L);
        when(positionRepository.countByDepartment_Career_CareerId(1L)).thenReturn(2L);

        when(careerRepository.save(any(Career.class))).thenReturn(c);

        assertDoesNotThrow(() -> careerService.delete(1L, true));
        assertTrue(c.getDeleted());
        assertNotNull(c.getDeletedAt());
        verify(careerRepository, times(1)).save(c);
    }
}