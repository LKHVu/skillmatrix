package com.das.skillmatrix.dto.response;

import java.util.List;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDetailResponse {
    private Long departmentId;
    private String name;
    private String description;
    private Long careerId;
    private GeneralStatus status;
    private long totalTeams;
    private List<TeamBrief> teams;
}
