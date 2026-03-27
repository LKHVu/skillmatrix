package com.das.skillmatrix.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.das.skillmatrix.entity.GeneralStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    // Basic Info
    private Long userId;
    private String email;
    private String fullName;
    private String userAvatar;
    private String phone;
    private String role;
    private GeneralStatus status;
    private String deactiveType;
    private LocalDateTime deactiveUntil;
    private List<PositionBrief> positions;

    // Scope Info
    private Long careerId;
    private String careerName;
    private Long departmentId;
    private String departmentName;
    private Long teamId;
    private String teamName;

    private LocalDateTime createdAt;
}
