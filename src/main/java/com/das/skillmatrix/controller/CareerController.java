package com.das.skillmatrix.controller;

import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.CareerDeleteCheckResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.service.CareerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/careers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CareerResponse>> create(@Valid @RequestBody CareerRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.create(req), true, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CareerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CareerRequest req
    ) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.update(id, req), true, null));
    }

    @GetMapping("/{id}/delete-check")
    public ResponseEntity<ApiResponse<CareerDeleteCheckResponse>> deleteCheck(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.deleteCheck(id), true, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean confirm
    ) {
        careerService.delete(id, confirm);
        return ResponseEntity.ok(new ApiResponse<>("Delete success", true, null));
    }

    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CareerResponse>>> list(
            @PageableDefault(size = 20, sort = "careerId", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.list(pageable), true, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CareerResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(careerService.detail(id), true, null));
    }
}