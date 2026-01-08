package com.das.skillmatrix.service;

import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.CareerDeleteCheckResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.dto.response.DepartmentBrief;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.PositionRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    public CareerResponse create(CareerRequest req) {
        String name = req.getName().trim();

        var opt = careerRepository.findFirstByNameIgnoreCase(name);
        if (opt.isPresent()) {
            Career existing = opt.get();
            if (!existing.getDeleted()) {
                throw new IllegalArgumentException("CAREER_NAME_EXISTS");
            }
            existing.setDeleted(false);
            existing.setDeletedAt(null);
            existing.setDescription(req.getDescription());
            Career saved = careerRepository.save(existing);
            long deptCount = departmentRepository.countByCareer_CareerId(saved.getCareerId());
            return new CareerResponse(saved.getCareerId(), saved.getName(), saved.getDescription(), deptCount);
        }

        Career c = new Career();
        c.setName(name);
        c.setDescription(req.getDescription());
        c.setDeleted(false);
        c.setDeletedAt(null);
        c = careerRepository.save(c);

        return new CareerResponse(c.getCareerId(), c.getName(), c.getDescription(), 0);
    }

    public CareerResponse update(Long id, CareerRequest req) {
        Career c = careerRepository.findByCareerIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));

        String newName = req.getName().trim();
        if (!c.getName().equalsIgnoreCase(newName)
                && careerRepository.existsByNameIgnoreCaseAndDeletedFalse(newName)) {
            throw new IllegalArgumentException("CAREER_NAME_EXISTS");
        }

        c.setName(newName);
        c.setDescription(req.getDescription());
        c = careerRepository.save(c);

        long deptCount = departmentRepository.countByCareer_CareerId(id);
        return new CareerResponse(c.getCareerId(), c.getName(), c.getDescription(), deptCount);
    }

    @Transactional(readOnly = true)
    public CareerDeleteCheckResponse deleteCheck(Long id) {
        Career c = careerRepository.findByCareerIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));

        var deps = departmentRepository.findByCareer_CareerId(id);
        var depBriefs = deps.stream()
                .map(d -> new DepartmentBrief(d.getDepartmentId(), d.getName()))
                .toList();

        long deptCount = depBriefs.size();
        long posCount = positionRepository.countByDepartment_Career_CareerId(id);

        boolean requireConfirm = (deptCount > 0 || posCount > 0);

        return new CareerDeleteCheckResponse(
                c.getCareerId(),
                c.getName(),
                deptCount,
                depBriefs,
                posCount,
                requireConfirm
        );
    }

    public void delete(Long id, boolean confirm) {
        Career c = careerRepository.findByCareerIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));

        long deptCount = departmentRepository.countByCareer_CareerId(id);
        long posCount  = positionRepository.countByDepartment_Career_CareerId(id);

        boolean requireConfirm = (deptCount > 0 || posCount > 0);
        if (requireConfirm && !confirm) {
            throw new IllegalStateException("CAREER_DELETE_CONFIRM_REQUIRED");
        }

        c.setDeleted(true);
        c.setDeletedAt(LocalDateTime.now());
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
    public CareerResponse detail(Long id) {
        Career c = careerRepository.findByCareerIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("CAREER_NOT_FOUND"));

        long count = departmentRepository.countByCareer_CareerId(id);
        return new CareerResponse(c.getCareerId(), c.getName(), c.getDescription(), count);
    }
}