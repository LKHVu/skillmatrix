package com.das.skillmatrix.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CareerDeleteCheckResponse {
    private Long careerId;
    private String name;
    private long departmentsCount;
    private List<DepartmentBrief> departments;
    private long positionsCount;
    private boolean requireConfirm;
}