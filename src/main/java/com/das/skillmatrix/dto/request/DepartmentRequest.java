package com.das.skillmatrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {
    @NotBlank(message = "NAME_REQUIRED")
    private String name;

    private String description;

    @NotNull(message = "CAREER_ID_REQUIRED")
    private Long careerId;
}
