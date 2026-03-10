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
    private GeneralStatus status;
    private LocalDateTime createdAt;
    private long memberCount;
    private Department department;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Department {
        private Long departmentId;
        private String name;
    }
}
