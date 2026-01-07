package com.das.skillmatrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long teamId;
    private String name;
    private String description;
    private Manager manager;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Manager {
        private Long userId;
        private String email;
        private String fullName;
    }
}
