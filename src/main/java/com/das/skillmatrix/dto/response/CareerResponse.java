package com.das.skillmatrix.dto.response;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CareerResponse {
    private Long careerId;
    private String name;
    private String description;
    private GeneralStatus status;
}