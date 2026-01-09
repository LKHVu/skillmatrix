package com.das.skillmatrix.service;

import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.CareerDetailResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.dto.response.DepartmentBrief;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.CareerStatus;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;
    private final DepartmentRepository departmentRepository;

    public CareerResponse create(CareerRequest req) {
        String name = normalizeName(req.getName());
        if (careerRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("CAREER_NAME_EXISTS");
        }
        Career c = new Career();
        c.setName(name);
        c.setDescription(req.getDescription());
        c = careerRepository.save(c);
        return new CareerResponse(c.getCareerId(), c.getName(), c.getDescription(), c.getStatus());
    }

    public CareerResponse update(Long id, CareerRequest req) {
        Career c = getActiveCareerOrThrow(id);
        String newName = normalizeName(req.getName());
        if (!c.getName().equalsIgnoreCase(newName) && careerRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("CAREER_NAME_EXISTS");
        }
        c.setName(newName);
        c.setDescription(req.getDescription());
        careerRepository.save(c);
        return new CareerResponse(c.getCareerId(), c.getName(), c.getDescription(), c.getStatus());
    }

    public void delete(Long id) {
        Career c = getActiveCareerOrThrow(id);
        long deptCount = departmentRepository.countByCareer_CareerId(id);
        if (deptCount > 0) {
            c.setStatus(CareerStatus.DEACTIVE);
            c.setDeActiveAt(LocalDateTime.now());
        } else {
            c.setStatus(CareerStatus.DELETED);
            c.setDeletedAt(LocalDateTime.now());
        }
        careerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<CareerResponse> list(Pageable pageable) {
        var page = careerRepository.findCareerResponses(pageable);
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @Transactional(readOnly = true)
    public CareerDetailResponse detail(Long id) {
        Career c = getVisibleCareerOrThrow(id);
        List<DepartmentBrief> depBriefs = getDepartmentBriefs(id);
        return new CareerDetailResponse(
                c.getCareerId(),
                c.getName(),
                c.getDescription(),
                depBriefs.size(),
                depBriefs,
                c.getStatus()
        );
    }

    private Career getActiveCareerOrThrow(Long id) {
        return careerRepository.findByCareerIdAndStatus(id, CareerStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));
    }

    private Career getVisibleCareerOrThrow(Long id) {
        return careerRepository.findByCareerIdAndStatusIn(id, List.of(CareerStatus.ACTIVE, CareerStatus.DEACTIVE))
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));
    }

    private List<DepartmentBrief> getDepartmentBriefs(Long careerId) {
        return departmentRepository.findByCareer_CareerId(careerId).stream()
                .map(d -> new DepartmentBrief(d.getDepartmentId(), d.getName()))
                .toList();
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }
}