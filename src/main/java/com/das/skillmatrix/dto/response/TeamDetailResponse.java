package com.das.skillmatrix.dto.response;

import java.time.LocalDateTime;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamDetailResponse {
    private Long teamId;
    private String name;
    private String description;
    private Long departmentId;
    private String departmentName;
    private GeneralStatus status;
    private LocalDateTime createdAt;
    private long memberCount;
}