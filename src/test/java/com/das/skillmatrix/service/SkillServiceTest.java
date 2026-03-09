package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.das.skillmatrix.dto.request.SkillRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.SkillResponse;
import com.das.skillmatrix.entity.Skill;
import com.das.skillmatrix.entity.SkillStatus;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.PositionSkillRepository;
import com.das.skillmatrix.repository.SkillRepository;
import com.das.skillmatrix.repository.UserSkillRepository;

@ExtendWith(MockitoExtension.class)
public class SkillServiceTest {
    @Mock
    private SkillRepository skillRepository;

    @Mock
    private PositionSkillRepository positionSkillRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private SkillService skillService;

    private static Skill skill(Long id, String name, String description, SkillStatus status) {
        Skill s = new Skill();
        s.setSkillId(id);
        s.setName(name);
        s.setDescription(description);
        s.setStatus(status);
        return s;
    }

    @Test
    @DisplayName("createSkill() should save skill and return SkillResponse")
    void createSkill_shouldSaveAndReturnResponse() {
        SkillRequest req = new SkillRequest("Java Core", "desc");

        when(skillRepository.findByNameIgnoreCase("Java Core")).thenReturn(null);
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setSkillId(1L);
            return s;
        });

        SkillResponse res = skillService.createSkill(req);

        assertEquals(1L, res.getSkillId());
        assertEquals("Java Core", res.getName());
        assertEquals("desc", res.getDescription());
        assertEquals(SkillStatus.ACTIVE, res.getStatus());

        verify(skillRepository).findByNameIgnoreCase("Java Core");
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    @DisplayName("createSkill() should throw when name already exists")
    void createSkill_shouldThrow_whenNameExists() {
        SkillRequest req = new SkillRequest("Java", "desc");
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(skill(1L, "Java", "desc", SkillStatus.ACTIVE));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> skillService.createSkill(req));
        assertEquals("SKILL_ALREADY_EXISTS", ex.getMessage());

        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSkill() should update skill and return SkillResponse")
    void updateSkill_shouldUpdateAndReturnResponse() {
        Skill existing = skill(1L, "Old", "Old desc", SkillStatus.ACTIVE);

        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByNameIgnoreCase("New Name")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillResponse res = skillService.updateSkill(1L, new SkillRequest("New Name", "New desc"));

        assertEquals(1L, res.getSkillId());
        assertEquals("New Name", res.getName());
        assertEquals("New desc", res.getDescription());
        assertEquals(SkillStatus.ACTIVE, res.getStatus());

        verify(skillRepository).save(existing);
    }

    @Test
    @DisplayName("updateSkill() should throw when skill not found")
    void updateSkill_shouldThrow_whenNotFound() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> skillService.updateSkill(99L, new SkillRequest("A", "B")));
        assertEquals("SKILL_NOT_FOUND", ex.getMessage());

        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSkill() should throw when updating name to an existing skill name")
    void updateSkill_shouldThrow_whenNameAlreadyExists() {
        Skill existing = skill(1L, "SQL", "old desc", SkillStatus.ACTIVE);
        
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> skillService.updateSkill(1L, new SkillRequest("Java", "new desc"))
        );

        assertEquals("SKILL_ALREADY_EXISTS", ex.getMessage());
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("deleteSkill() should set INACTIVE when referenced by position_skills or user_skills")
    void deleteSkill_shouldSetInactive_whenReferenced() {
        Skill s = skill(1L, "Java", "desc", SkillStatus.ACTIVE);

        when(skillRepository.findById(1L)).thenReturn(Optional.of(s));
        when(positionSkillRepository.existsBySkill_SkillId(1L)).thenReturn(true);
        when(userSkillRepository.existsBySkill_SkillId(1L)).thenReturn(false);

        skillService.deleteSkill(1L);

        assertEquals(SkillStatus.INACTIVE, s.getStatus());
        assertNotNull(s.getInactiveAt());
        assertNull(s.getDeletedAt());
    }

    @Test
    @DisplayName("deleteSkill() should set DELETED when not referenced anywhere")
    void deleteSkill_shouldSetDeleted_whenNotReferenced() {
        Skill s = skill(1L, "Java", "desc", SkillStatus.ACTIVE);

        when(skillRepository.findById(1L)).thenReturn(Optional.of(s));
        when(positionSkillRepository.existsBySkill_SkillId(1L)).thenReturn(false);
        when(userSkillRepository.existsBySkill_SkillId(1L)).thenReturn(false);

        skillService.deleteSkill(1L);

        assertEquals(SkillStatus.DELETED, s.getStatus());
        assertNotNull(s.getDeletedAt());
    }

    @Test
    @DisplayName("listSkills() should return PageResponse with ACTIVE + INACTIVE skills")
    void listSkills_shouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);

        Skill s1 = skill(1L, "Java", "d1", SkillStatus.ACTIVE);
        Skill s2 = skill(2L, "SQL", "d2", SkillStatus.INACTIVE);

        Page<Skill> page = new PageImpl<>(List.of(s1, s2), pageable, 2);

        when(skillRepository.findByStatusIn(eq(List.of(SkillStatus.ACTIVE, SkillStatus.INACTIVE)), eq(pageable)))
                .thenReturn(page);

        PageResponse<SkillResponse> res = skillService.listSkills(pageable);

        assertNotNull(res);
        assertEquals(2, res.getItems().size());
        assertEquals(0, res.getPage());
        assertEquals(10, res.getSize());
        assertEquals(2L, res.getTotalElements());
        assertEquals(1, res.getTotalPages());
        assertFalse(res.isHasNext());
        assertFalse(res.isHasPrevious());
    }

    @Test
    @DisplayName("getSkillById() should return SkillResponse when found")
    void getSkillById_shouldReturnResponse() {
        Skill s = skill(1L, "Java", "desc", SkillStatus.ACTIVE);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(s));

        SkillResponse res = skillService.getSkillById(1L);

        assertEquals(1L, res.getSkillId());
        assertEquals("Java", res.getName());
        assertEquals("desc", res.getDescription());
        assertEquals(SkillStatus.ACTIVE, res.getStatus());
    }
}