package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.annotation.LogActivity;
import com.das.skillmatrix.dto.request.CareerFilterRequest;
import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.CareerDetailResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.dto.response.DepartmentBrief;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.repository.specification.CareerSpecification;


import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final BusinessChangeLogService businessChangeLogService;

    @LogActivity(action = "CREATE_CAREER", entityType = "CAREER")
    public CareerResponse create(CareerRequest req) {
        String name = normalizeName(req.getName());
        if (careerRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("CAREER_NAME_EXISTS");
        }
        Career c = new Career();
        c.setName(name);
        c.setCareerType(req.getCareerType());
        c.setDescription(req.getDescription());
        c = careerRepository.save(c);
        return new CareerResponse(c.getCareerId(), c.getName(), c.getCareerType(), c.getDescription(), c.getStatus(), c.getCreatedAt());
    }

    @LogActivity(action = "UPDATE_CAREER", entityType = "CAREER")
    public CareerResponse update(Long id, CareerRequest req) {
        Career c = getActiveCareerOrThrow(id);
        String newName = normalizeName(req.getName());
        if (!c.getName().equalsIgnoreCase(newName) && careerRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("CAREER_NAME_EXISTS");
        }
        c.setName(newName);
        c.setCareerType(req.getCareerType());
        c.setDescription(req.getDescription());
        careerRepository.save(c);
        return new CareerResponse(c.getCareerId(), c.getName(), c.getCareerType(), c.getDescription(), c.getStatus(), c.getCreatedAt());
    }

    @LogActivity(action = "DELETE_CAREER", entityType = "CAREER")
    public void delete(Long id) {
        Career c = getActiveCareerOrThrow(id);
        String oldStatus = c.getStatus().name();
        long deptCount = departmentRepository.countByCareer_CareerId(id);
        if (deptCount > 0) {
            c.setStatus(GeneralStatus.DEACTIVE);
            c.setDeActiveAt(LocalDateTime.now());
        } else {
            c.setStatus(GeneralStatus.DELETED);
            c.setDeletedAt(LocalDateTime.now());
        }
        careerRepository.save(c);
        businessChangeLogService.log(
                "CHANGE_CAREER_STATUS", "CAREER", id,
                "status", oldStatus, c.getStatus().name());
    }

    @Transactional(readOnly = true)
    public PageResponse<CareerResponse> list(CareerFilterRequest filter, Pageable pageable) {
        var spec = CareerSpecification.filterCareers(filter);
        var page = careerRepository.findAll(spec, pageable);
        
        List<CareerResponse> content = page.getContent().stream()
            .map(c -> new CareerResponse(
                c.getCareerId(),
                c.getName(),
                c.getCareerType(),
                c.getDescription(),
                c.getStatus(),
                c.getCreatedAt()
            )).toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }

    @Transactional(readOnly = true)
    public CareerDetailResponse detail(Long id) {
        Career c = getVisibleCareerOrThrow(id);
        List<DepartmentBrief> depBriefs = getDepartmentBriefs(id);
        return new CareerDetailResponse(
                c.getCareerId(),
                c.getName(),
                c.getCareerType(),
                c.getDescription(),
                depBriefs.size(),
                depBriefs,
                c.getStatus(),
                c.getCreatedAt());
    }

    private Career getActiveCareerOrThrow(Long id) {
        return careerRepository.findByCareerIdAndStatus(id, GeneralStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));
    }

    private Career getVisibleCareerOrThrow(Long id) {
        return careerRepository.findByCareerIdAndStatusIn(id, List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE))
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));
    }

    private List<DepartmentBrief> getDepartmentBriefs(Long careerId) {
        return departmentRepository.findDepartmentBriefsByCareerId(careerId);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    @LogActivity(action = "ADD_CAREER_MANAGER", entityType = "CAREER")
    public void addManager(Long careerId, Long userId) {
        Career career = getActiveCareerOrThrow(careerId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (!GeneralStatus.ACTIVE.name().equals(user.getStatus().name())) {
            throw new IllegalArgumentException("USER_NOT_ACTIVE");
        }
        if (!"MANAGER_CAREER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("INVALID_MANAGER_ROLE");
        }
        boolean alreadyManager = career.getManagers().stream()
                .anyMatch(u -> u.getUserId().equals(userId));
        if (!alreadyManager) {
            career.getManagers().add(user);
            careerRepository.save(career);
            businessChangeLogService.log(
                    "ADD_CAREER_MANAGER", "CAREER", careerId,
                    "managerId", null, userId.toString());
        }
    }

    @LogActivity(action = "REMOVE_CAREER_MANAGER", entityType = "CAREER")
    public void removeManager(Long careerId, Long userId) {
        Career career = getActiveCareerOrThrow(careerId);
        boolean removed = career.getManagers().removeIf(u -> u.getUserId().equals(userId));
        if (removed) {
            careerRepository.save(career);
            businessChangeLogService.log(
                    "REMOVE_CAREER_MANAGER", "CAREER", careerId,
                    "managerId", userId.toString(), null);
        }
    }
}
