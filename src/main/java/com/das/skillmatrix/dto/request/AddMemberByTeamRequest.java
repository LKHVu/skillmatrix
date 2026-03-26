package com.das.skillmatrix.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberByTeamRequest {

    @NotNull(message = "teamId is required")
    private Long teamId;

    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    @NotNull(message = "positionId is required")
    private Long positionId;
}