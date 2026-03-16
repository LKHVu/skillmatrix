package com.das.skillmatrix.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.das.skillmatrix.dto.request.CareerFilterRequest;
import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.CareerDetailResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.service.CareerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/careers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CareerResponse>> create(@Valid @RequestBody CareerRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.create(req), true, null));
    }

    @PreAuthorize("@permissionService.checkCareerAccess(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CareerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CareerRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.update(id, req), true, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        careerService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Delete success", true, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CareerResponse>>> list(
            @ModelAttribute CareerFilterRequest filter,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.list(filter, pageable), true, null));
    }

    @PreAuthorize("@permissionService.checkCareerAccess(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CareerDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.detail(id), true, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/managers/{userId}")
    public ResponseEntity<ApiResponse<Void>> addManager(@PathVariable Long id, @PathVariable Long userId) {
        careerService.addManager(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/managers/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeManager(@PathVariable Long id, @PathVariable Long userId) {
        careerService.removeManager(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }
}