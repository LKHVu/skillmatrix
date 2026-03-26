package com.das.skillmatrix.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.das.skillmatrix.dto.request.TeamFilterRequest;
import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.service.TeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PreAuthorize("@permissionService.checkDepartmentAccess(#req.departmentId)")
    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody TeamRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(teamService.create(req), true, null));
    }

    @PreAuthorize("@permissionService.checkTeamAccess(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(teamService.update(id, req), true, null));
    }

    @PreAuthorize("@permissionService.canManageTeam(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Delete success", true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_DEPARTMENT', 'MANAGER_CAREER', 'MANAGER_TEAM')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeamResponse>>> list(
            @ModelAttribute TeamFilterRequest filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(teamService.list(filter, pageable), true, null));
    }

    @PreAuthorize("@permissionService.checkTeamViewAccess(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(teamService.detail(id), true, null));
    }

    @PreAuthorize("@permissionService.canManageTeam(#id)")
    @PostMapping("/{id}/managers/{userId}")
    public ResponseEntity<ApiResponse<Void>> addManager(@PathVariable Long id, @PathVariable Long userId) {
        teamService.addManager(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }

    @PreAuthorize("@permissionService.canManageTeam(#id)")
    @DeleteMapping("/{id}/managers/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeManager(@PathVariable Long id, @PathVariable Long userId) {
        teamService.removeManager(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }
}
