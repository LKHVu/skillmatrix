package com.das.skillmatrix.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private Long teamId;
    private String teamName;
    private Long positionId;
    private String positionName;
    private LocalDateTime createdAt;
}