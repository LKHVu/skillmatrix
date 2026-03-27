package com.das.skillmatrix.dto.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Role is required")
    private String role;

    private Long careerId;
    private Long departmentId;
    private Long teamId;

    private List<Long> positionIds; // validated in service (not required for ADMIN)
}
