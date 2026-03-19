package com.das.skillmatrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentManagerResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String userAvatar;
    private String role;
    private boolean isRemovable;
}