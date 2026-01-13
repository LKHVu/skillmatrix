package com.das.skillmatrix.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.das.skillmatrix.dto.request.DepartmentRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.DepartmentDetailResponse;
import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.service.DepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PreAuthorize("@permissionService.checkCareerAccess(#req.careerId)")
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(departmentService.create(req), true, null));
    }

    @PreAuthorize("@permissionService.checkDepartmentAccess(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(@PathVariable Long id,
            @Valid @RequestBody DepartmentRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(departmentService.update(id, req), true, null));
    }

    @PreAuthorize("@permissionService.canManageDepartment(#departmentId)")
    @PostMapping("/{departmentId}/managers")
    public ResponseEntity<ApiResponse<Void>> assignManagers(@PathVariable Long departmentId,
            @RequestBody java.util.List<Long> managerIds) {
        departmentService.assignManagers(departmentId, managerIds);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }

    @PreAuthorize("@permissionService.canManageDepartment(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Delete success", true, null));
    }

    @PreAuthorize("@permissionService.canViewDepartmentList(#careerId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> list(
            @RequestParam Long careerId,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(departmentService.listByCareer(careerId, pageable), true, null));
    }

    @PreAuthorize("@permissionService.canViewDepartmentDetail(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(departmentService.detail(id), true, null));
    }
}