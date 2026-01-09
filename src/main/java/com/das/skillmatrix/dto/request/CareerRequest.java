package com.das.skillmatrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CareerRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}