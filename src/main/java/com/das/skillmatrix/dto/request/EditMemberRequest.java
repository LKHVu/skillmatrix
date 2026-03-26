package com.das.skillmatrix.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditMemberRequest {

    @NotNull(message = "positionId is required")
    private Long positionId;
}