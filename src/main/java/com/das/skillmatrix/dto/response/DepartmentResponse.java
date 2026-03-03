package com.das.skillmatrix.dto.response;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentResponse {
    private Long departmentId;
    private String name;
    private String description;
    private Long careerId;
    private GeneralStatus status;
}
