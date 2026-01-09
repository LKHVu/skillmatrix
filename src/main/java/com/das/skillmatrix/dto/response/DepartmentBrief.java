package com.das.skillmatrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentBrief {
    private Long departmentId;
    private String name;
}