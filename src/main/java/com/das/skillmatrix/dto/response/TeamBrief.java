package com.das.skillmatrix.dto.response;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamBrief {
    private Long teamId;
    private String name;
    private GeneralStatus status;
}
