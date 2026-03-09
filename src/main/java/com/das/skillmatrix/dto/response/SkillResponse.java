package com.das.skillmatrix.dto.response;

import com.das.skillmatrix.entity.SkillStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponse {
    private Long skillId;
    private String name;
    private String description;
    private SkillStatus status;
}
