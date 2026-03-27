package com.das.skillmatrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeactivateUserRequest {
    @NotBlank(message = "Action is required")
    private String action; // "DEACTIVE" or "DELETE"
    
    private String deactiveType; // "TEMPORARY" or "UNLIMITED"
    private String duration; // "3_DAYS", "7_DAYS", "1_MONTH", "3_MONTHS", "6_MONTHS", "1_YEAR"
}
