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
public class UserResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String userAvatar;
    private String role;
    private List<PositionBrief> positions;
    private GeneralStatus status;
    private LocalDateTime createdAt;
}
