package com.das.skillmatrix.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CareerDetailResponse {
    private Long careerId;
    private String name;
    private String careerType;
    private String description;
    private long departmentsCount;
    private List<DepartmentBrief> departments;
    private GeneralStatus status;
    private LocalDateTime createdAt;
}