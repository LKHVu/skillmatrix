package com.das.skillmatrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CareerResponse {
    private Long careerId;
    private String name;
    private String description;
    private long departmentsCount;
}