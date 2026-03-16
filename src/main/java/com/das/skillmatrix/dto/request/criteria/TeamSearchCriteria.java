package com.das.skillmatrix.dto.request.criteria;

import lombok.Data;

@Data
public class TeamSearchCriteria {
    private Long departmentId;
    private String keyword;
    private String status;
    private String dateFilter;
}
