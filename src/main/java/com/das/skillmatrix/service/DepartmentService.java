package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.annotation.LogActivity;
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
import com.das.skillmatrix.repository.specification.DepartmentSpecification;

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
    private final BusinessChangeLogService businessChangeLogService;

    @LogActivity(action = "CREATE_DEPARTMENT", entityType = "DEPARTMENT")
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
                department.getCareer().getName(),
                department.getStatus(),
                department.getCreatedAt());
    }

    @LogActivity(action = "UPDATE_DEPARTMENT", entityType = "DEPARTMENT")
    public DepartmentResponse update(Long id, DepartmentRequest req) {
        Department department = getActiveDepartmentOrThrow(id);
        Long oldCareerId = department.getCareer().getCareerId();
        String newName = normalizeName(req.getName());
        Long newCareerId = req.getCareerId();
        if (!oldCareerId.equals(newCareerId)) {
            if (permissionService.isManagerDepartmentOnly()) {
                throw new AccessDeniedException("MANAGER_DEPARTMENT_CANNOT_CHANGE_CAREER");
            }
            Career targetCareer = careerRepository.findByCareerIdAndStatus(newCareerId, GeneralStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("TARGET_CAREER_NOT_FOUND_OR_INACTIVE"));
            if (!permissionService.canMoveDepartment(oldCareerId, newCareerId)) {
                throw new AccessDeniedException("ACCESS_DENIED_TO_MIGRATE_CAREER");
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
        if (!oldCareerId.equals(newCareerId)) {
            businessChangeLogService.log(
                    "MIGRATE_DEPARTMENT_CAREER", "DEPARTMENT", id,
                    "careerId", oldCareerId.toString(), newCareerId.toString());
        }
        return new DepartmentResponse(
                department.getDepartmentId(),
                department.getName(),
                department.getDescription(),
                department.getCareer().getCareerId(),
                department.getCareer().getName(),
                department.getStatus(),
                department.getCreatedAt());
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    @LogActivity(action = "DELETE_DEPARTMENT", entityType = "DEPARTMENT")
    public void delete(Long id) {
        Department department = getActiveDepartmentOrThrow(id);
        String oldStatus = department.getStatus().name();
        long teamCount = teamRepository.countByDepartment_DepartmentId(id);
        if (teamCount > 0) {
            department.setStatus(GeneralStatus.DEACTIVE);
            department.setDeActiveAt(LocalDateTime.now());
        } else {
            department.setStatus(GeneralStatus.DELETED);
            department.setDeletedAt(LocalDateTime.now());
        }
        departmentRepository.save(department);
        businessChangeLogService.log(
                "CHANGE_DEPARTMENT_STATUS", "DEPARTMENT", id,
                "status", oldStatus, department.getStatus().name());
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> list(Long careerId, DepartmentFilterRequest filter, Pageable pageable) {
        Career career = careerRepository.findById(careerId)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));
        if (career.getStatus() == GeneralStatus.DELETED) {
            throw new IllegalArgumentException("CAREER_NOT_FOUND");
        }
        var spec = DepartmentSpecification.filterDepartmentsByCareer(careerId, filter);
        var page = departmentRepository.findAll(spec, pageable);
        List<DepartmentResponse> content = page.getContent().stream()
                .map(d -> new DepartmentResponse(
                        d.getDepartmentId(), d.getName(), d.getDescription(),
                        d.getCareer().getCareerId(), d.getCareer().getName(),
                        d.getStatus(), d.getCreatedAt()
                )).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    @Transactional(readOnly = true)
    public DepartmentDetailResponse detail(Long id) {
        Department department = getVisibleDepartmentOrThrow(id);
        long teamCount = teamRepository.countByDepartment_DepartmentIdAndStatusIn(id, List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
        return new DepartmentDetailResponse(
                department.getDepartmentId(),
                department.getName(),
                department.getDescription(),
                department.getCareer().getCareerId(),
                department.getCareer().getName(),
                department.getStatus(),
                department.getCreatedAt(),
                teamCount);
    }

    @LogActivity(action = "ADD_DEPARTMENT_MANAGER", entityType = "DEPARTMENT")
    public void addManager(Long departmentId, Long userId) {
        Department department = getActiveDepartmentOrThrow(departmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (user.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("USER_NOT_ACTIVE");
        }
        if (!"Manager Department".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("INVALID_MANAGER_ROLE");
        }
        boolean alreadyManager = department.getManagers().stream()
                .anyMatch(u -> u.getUserId().equals(userId));
        if (!alreadyManager) {
            department.getManagers().add(user);
            departmentRepository.save(department);
            businessChangeLogService.log(
                    "ADD_DEPARTMENT_MANAGER", "DEPARTMENT", departmentId,
                    "managerId", null, userId.toString());
        }
    }

    @LogActivity(action = "REMOVE_DEPARTMENT_MANAGER", entityType = "DEPARTMENT")
    public void removeManager(Long departmentId, Long userId) {
        Department department = getActiveDepartmentOrThrow(departmentId);
        boolean removed = department.getManagers().removeIf(u -> u.getUserId().equals(userId));
        if (removed) {
            departmentRepository.save(department);
            businessChangeLogService.log(
                    "REMOVE_DEPARTMENT_MANAGER", "DEPARTMENT", departmentId,
                    "managerId", userId.toString(), null);
        }
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