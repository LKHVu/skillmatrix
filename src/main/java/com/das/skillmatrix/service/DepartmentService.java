package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.dto.request.DepartmentRequest;
import com.das.skillmatrix.dto.response.DepartmentDetailResponse;
import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamBrief;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CareerRepository careerRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final TeamRepository teamRepository;

    public DepartmentResponse create(DepartmentRequest req) {
        String name = normalizeName(req.getName());
        Long careerId = req.getCareerId();
        Career career = careerRepository
                .findByCareerIdAndStatus(careerId, GeneralStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND_OR_INACTIVE"));
        if (departmentRepository.existsByNameIgnoreCaseAndCareer_CareerId(name, careerId)) {
            throw new IllegalArgumentException("DEPARTMENT_NAME_EXISTS_IN_CAREER");
        }
        Department department = new Department();
        department.setName(name);
        department.setDescription(req.getDescription());
        department.setCareer(career);
        department = departmentRepository.save(department);
        return new DepartmentResponse(
                department.getDepartmentId(),
                department.getName(),
                department.getDescription(),
                department.getCareer().getCareerId(),
                department.getStatus());
    }

    public void assignManagers(Long departmentId, List<Long> managerIds) {
        Department department = getActiveDepartmentOrThrow(departmentId);
        List<User> managers = userRepository.findAllById(managerIds);
        department.setManagers(managers);
        departmentRepository.save(department);
    }

    public DepartmentResponse update(Long id, DepartmentRequest req) {
        Department department = getActiveDepartmentOrThrow(id);

        String newName = normalizeName(req.getName());
        Long newCareerId = req.getCareerId();
        if (!department.getCareer().getCareerId().equals(newCareerId)) {
            Career targetCareer = careerRepository.findByCareerIdAndStatus(newCareerId, GeneralStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("TARGET_CAREER_NOT_FOUND_OR_INACTIVE"));
            if (!permissionService.canMoveDepartment(department.getCareer().getCareerId(), newCareerId)) {
                throw new org.springframework.security.access.AccessDeniedException("ACCESS_DENIED_TO_MIGRATE_CAREER");
            }
            department.setCareer(targetCareer);
        }
        if (!department.getName().equalsIgnoreCase(newName)) {
            if (departmentRepository.existsByNameIgnoreCaseAndCareer_CareerId(newName,
                    department.getCareer().getCareerId())) {
                throw new IllegalArgumentException("DEPARTMENT_NAME_EXISTS_IN_CAREER");
            }
            department.setName(newName);
        }
        department.setDescription(req.getDescription());
        department = departmentRepository.save(department);
        return new DepartmentResponse(
                department.getDepartmentId(),
                department.getName(),
                department.getDescription(),
                department.getCareer().getCareerId(),
                department.getStatus());
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    public void delete(Long id) {
        Department department = getActiveDepartmentOrThrow(id);

        long teamCount = teamRepository.countByDepartment_DepartmentId(id);
        if (teamCount > 0) {
            department.setStatus(GeneralStatus.DEACTIVE);
            department.setDeActiveAt(LocalDateTime.now());
        } else {
            department.setStatus(GeneralStatus.DELETED);
            department.setDeletedAt(LocalDateTime.now());
        }
        departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listByCareer(Long careerId, Pageable pageable) {
        Career career = careerRepository.findById(careerId)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));
        if (career.getStatus() == GeneralStatus.DELETED) {
            throw new IllegalArgumentException("CAREER_NOT_FOUND");
        }
        var page = departmentRepository.findDepartmentResponsesByCareerId(careerId, pageable);
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }

    @Transactional(readOnly = true)
    public DepartmentDetailResponse detail(Long id) {
        Department department = getVisibleDepartmentOrThrow(id);
        List<TeamBrief> teams = getTeamBriefs(id);
        return new DepartmentDetailResponse(
                department.getDepartmentId(),
                department.getName(),
                department.getDescription(),
                department.getCareer().getCareerId(),
                department.getStatus(),
                teams.size(),
                teams);
    }

    private List<TeamBrief> getTeamBriefs(Long departmentId) {
        return teamRepository.findByDepartment_DepartmentId(departmentId).stream()
                .map(t -> new TeamBrief(t.getTeamId(), t.getName(), t.getStatus()))
                .toList();
    }

    private Department getActiveDepartmentOrThrow(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DEPARTMENT_NOT_FOUND"));
        if (department.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("DEPARTMENT_NOT_ACTIVE");
        }
        return department;
    }

    private Department getVisibleDepartmentOrThrow(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DEPARTMENT_NOT_FOUND"));
        if (department.getStatus() == GeneralStatus.DELETED) {
            throw new IllegalArgumentException("DEPARTMENT_NOT_ACTIVE");
        }
        return department;
    }
}
