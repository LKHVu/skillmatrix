package com.das.skillmatrix.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.das.skillmatrix.dto.request.CreateUserRequest;
import com.das.skillmatrix.dto.request.DeactivateUserRequest;
import com.das.skillmatrix.dto.request.UpdateUserRequest;
import com.das.skillmatrix.dto.request.UserFilterRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.UserDetailResponse;
import com.das.skillmatrix.dto.response.UserResponse;
import com.das.skillmatrix.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER_CAREER','MANAGER_DEPARTMENT','MANAGER_TEAM')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @ModelAttribute UserFilterRequest filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserResponse> pageRes = userService.list(filter, pageable);
        return ResponseEntity.ok(new ApiResponse<>(pageRes, true, null));
    }

    @PreAuthorize("@permissionService.checkTeamViewAccess(#teamId)")
    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listByTeam(
            @PathVariable Long teamId,
            @ModelAttribute UserFilterRequest filter,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserResponse> pageRes = userService.listByTeam(teamId, filter, pageable);
        return ResponseEntity.ok(new ApiResponse<>(pageRes, true, null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER_CAREER','MANAGER_DEPARTMENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest req) {
        UserResponse response = userService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response, true, null));
    }

    @PreAuthorize("@permissionService.canManageUser(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        UserResponse response = userService.update(id, req);
        return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }

    @PreAuthorize("@permissionService.canManageUser(#id)")
    @PostMapping("/{id}/deactivate-or-delete")
    public ResponseEntity<ApiResponse<String>> deactivateOrDelete(
            @PathVariable Long id, @Valid @RequestBody DeactivateUserRequest req) {
        userService.deactivateOrDelete(id, req);
        return ResponseEntity.ok(new ApiResponse<>("Action " + req.getAction() + " completed successfully", true, null));
    }

    @PreAuthorize("@permissionService.canManageUser(#id)")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<UserResponse>> reactivate(@PathVariable Long id) {
        UserResponse response = userService.reactivate(id);
        return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }

    @PreAuthorize("@permissionService.checkUserViewAccess(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> detail(@PathVariable Long id) {
        UserDetailResponse response = userService.getDetail(id);
        return ResponseEntity.ok(new ApiResponse<>(response, true, null));
    }
}
