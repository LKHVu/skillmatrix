package com.das.skillmatrix.dto.request;

import com.das.skillmatrix.entity.GeneralStatus;
import lombok.Data;

@Data
public class TeamFilterRequest {
    private String keyword;
    private GeneralStatus status;
    private String dateModified;
    private Long departmentId;
}