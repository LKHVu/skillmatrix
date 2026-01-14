package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.das.skillmatrix.dto.request.SkillRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.SkillResponse;
import com.das.skillmatrix.entity.Skill;
import com.das.skillmatrix.entity.SkillStatus;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.PositionSkillRepository;
import com.das.skillmatrix.repository.SkillRepository;
import com.das.skillmatrix.repository.UserSkillRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class SkillService {
    private final SkillRepository skillRepository;
    private final PositionSkillRepository positionSkillRepository;
    private final UserSkillRepository userSkillRepository;

    public SkillService(SkillRepository skillRepository, PositionSkillRepository positionSkillRepository,
            UserSkillRepository userSkillRepository) {
        this.skillRepository = skillRepository;
        this.positionSkillRepository = positionSkillRepository;
        this.userSkillRepository = userSkillRepository;
    }
    // Convert Skill to SkillResponse
    private SkillResponse toSkillResponse(Skill skill) {
        return new SkillResponse(
                skill.getSkillId(),
                skill.getName(),
                skill.getDescription(),
                skill.getStatus());
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }


    public SkillResponse createSkill(SkillRequest skillRequest) {
        String name = normalizeName(skillRequest.getName());
        Skill existingSkill = skillRepository.findByNameIgnoreCase(name);
        if (existingSkill != null && (existingSkill.getStatus() == SkillStatus.ACTIVE || existingSkill.getStatus() == SkillStatus.INACTIVE)) {
            throw new IllegalArgumentException("SKILL_ALREADY_EXISTS");
        }
        Skill skill = new Skill();
        skill.setName(name);
        skill.setDescription(skillRequest.getDescription());
        return toSkillResponse(skillRepository.save(skill));
    }

    public SkillResponse updateSkill(Long id, SkillRequest skillRequest) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND"));
        String name = normalizeName(skillRequest.getName());
        if (skillRepository.existsByNameIgnoreCase(name) && !skill.getName().equalsIgnoreCase(name)
                && (skill.getStatus() == SkillStatus.ACTIVE || skill.getStatus() == SkillStatus.INACTIVE)) {
            throw new IllegalArgumentException("SKILL_ALREADY_EXISTS");
        }
        skill.setName(name);
        skill.setDescription(skillRequest.getDescription());
        return toSkillResponse(skillRepository.save(skill));
    }

    public void deleteSkill(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND"));
        boolean existsPositionSkill = positionSkillRepository.existsBySkill_SkillId(id);
        boolean existsUserSkill = userSkillRepository.existsBySkill_SkillId(id);
        if (existsPositionSkill || existsUserSkill) {
            skill.setStatus(SkillStatus.INACTIVE);
            skill.setInactiveAt(LocalDateTime.now());
        } else {
            skill.setStatus(SkillStatus.DELETED);
            skill.setDeletedAt(LocalDateTime.now());
        }
    }

    // List all skills with status ACTIVE or INACTIVE
    public PageResponse<SkillResponse> listSkills(Pageable pageable) {
        Page<Skill> skills = skillRepository.findByStatusIn(List.of(SkillStatus.ACTIVE, SkillStatus.INACTIVE), pageable);
        // Convert Skill to SkillResponse
        List<SkillResponse> skillResponses = skills.stream()
                .map(skill -> toSkillResponse(skill))
                .toList();
        return new PageResponse<>(
                skillResponses,
                skills.getNumber(),
                skills.getSize(),
                skills.getTotalElements(),
                skills.getTotalPages(),
                skills.hasNext(),
                skills.hasPrevious());
    }

    public SkillResponse getSkillById(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND"));
        return toSkillResponse(skill);
    }
}
