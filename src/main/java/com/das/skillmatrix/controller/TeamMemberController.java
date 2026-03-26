package com.das.skillmatrix.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.das.skillmatrix.dto.request.AddMemberByTeamRequest;
import com.das.skillmatrix.dto.request.AddMemberByUserRequest;
import com.das.skillmatrix.dto.request.EditMemberRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamMemberResponse;
import com.das.skillmatrix.service.TeamMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/team-members")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_CAREER', 'MANAGER_DEPARTMENT', 'MANAGER_TEAM') and @permissionService.checkMultiTeamAccess(#req.assignments.![teamId])")
    @PostMapping("/by-user")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> addByUser(
            @Valid @RequestBody AddMemberByUserRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(teamMemberService.addByUser(req), true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_CAREER', 'MANAGER_DEPARTMENT', 'MANAGER_TEAM') and @permissionService.checkTeamAccess(#req.teamId)")
    @PostMapping("/by-team")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> addByTeam(
            @Valid @RequestBody AddMemberByTeamRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(teamMemberService.addByTeam(req), true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_CAREER', 'MANAGER_DEPARTMENT', 'MANAGER_TEAM') and @permissionService.checkTeamMemberAccess(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EditMemberRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(teamMemberService.update(id, req), true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_CAREER', 'MANAGER_DEPARTMENT', 'MANAGER_TEAM') and @permissionService.checkTeamMemberAccess(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        teamMemberService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Delete success", true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER_CAREER', 'MANAGER_DEPARTMENT', 'MANAGER_TEAM') and @permissionService.checkTeamAccess(#teamId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeamMemberResponse>>> listByTeam(
            @RequestParam Long teamId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(teamMemberService.listByTeam(teamId, pageable), true, null));
    }
}
