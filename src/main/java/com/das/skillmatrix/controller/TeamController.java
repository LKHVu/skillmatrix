package com.das.skillmatrix.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.request.criteria.TeamSearchCriteria;
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

    @PreAuthorize("@permissionService.checkDepartmentAccess(#teamRequest.departmentId)")
    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamRequest teamRequest) {
        TeamResponse teamResponse = teamService.createTeam(teamRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(teamResponse, true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_DEPARTMENT', 'MANAGER_CAREER', 'MANAGER_TEAM')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeamResponse>>> getAllTeams(
        @ModelAttribute TeamSearchCriteria criteria,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<TeamResponse> pageResponse;
        if (criteria.getDepartmentId() != null) {
            pageResponse = teamService.getTeamsByDepartment(criteria, pageable);
        } else {
            pageResponse = teamService.getAllTeams(criteria, pageable);
        }
        return ResponseEntity.ok(new ApiResponse<>(pageResponse, true, null));
    }

    @PreAuthorize("@permissionService.checkTeamAccess(#teamId)")
    @PutMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable Long teamId,
            @Valid @RequestBody TeamRequest teamRequest) {
        TeamResponse teamResponse = teamService.updateTeam(teamId, teamRequest);
        return ResponseEntity.ok(new ApiResponse<>(teamResponse, true, null));
    }

    @PreAuthorize("@permissionService.checkTeamViewAccess(#teamId)")
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamById(@PathVariable Long teamId) {
        TeamDetailResponse teamDetailResponse = teamService.getTeamById(teamId);
        return ResponseEntity.ok(new ApiResponse<>(teamDetailResponse, true, null));
    }

    @PreAuthorize("@permissionService.canManageTeam(#teamId)")
    @DeleteMapping("/{teamId}")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }

    @PreAuthorize("@permissionService.canManageTeam(#teamId)")
    @PostMapping("/{teamId}/managers")
    public ResponseEntity<ApiResponse<Void>> assignManagers(@PathVariable Long teamId,
            @RequestBody java.util.List<Long> managerIds) {
        teamService.assignManagers(teamId, managerIds);
        return ResponseEntity.ok(new ApiResponse<>(null, true, null));
    }
}
