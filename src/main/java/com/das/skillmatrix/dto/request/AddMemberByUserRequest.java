package com.das.skillmatrix.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberByUserRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotEmpty(message = "assignments must not be empty")
    @Valid
    private List<Assignment> assignments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignment {
        @NotNull(message = "teamId is required")
        private Long teamId;

        @NotNull(message = "positionId is required")
        private Long positionId;
    }
}