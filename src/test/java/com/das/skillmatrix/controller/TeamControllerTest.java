package com.das.skillmatrix.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.das.skillmatrix.config.JwtAuthenticationFilter;
import com.das.skillmatrix.dto.request.TeamFilterRequest;
import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.service.PermissionService;
import com.das.skillmatrix.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TeamService teamService;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static TeamResponse sampleTeamResponse(long teamId) {
        return new TeamResponse(teamId, "Team Alpha", "desc",
                1L, "Dept One",
                GeneralStatus.ACTIVE, LocalDateTime.of(2025, 6, 1, 10, 0));
    }

    private static TeamDetailResponse sampleDetailResponse(long teamId) {
        return new TeamDetailResponse(teamId, "Team Alpha", "desc",
                1L, "Dept One",
                GeneralStatus.ACTIVE, LocalDateTime.of(2025, 6, 1, 10, 0), 5L);
    }

    @Test
    @DisplayName("POST /api/teams - should return 200 when valid")
    void create_success() throws Exception {
        when(teamService.create(any(TeamRequest.class))).thenReturn(sampleTeamResponse(1L));
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("Team Alpha", "desc", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.departmentId").value(1))
                .andExpect(jsonPath("$.data.departmentName").value("Dept One"));
        verify(teamService).create(any(TeamRequest.class));
    }

    @Test
    @DisplayName("POST /api/teams - should return 400 when validation fails")
    void create_validation() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":"desc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value(400));
        verify(teamService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/teams - should return 404 when department not found")
    void create_departmentNotFound() throws Exception {
        when(teamService.create(any(TeamRequest.class)))
                .thenThrow(new IllegalArgumentException("DEPARTMENT_NOT_FOUND"));
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("Team", "desc", 99L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Department not found"));
    }

    @Test
    @DisplayName("PUT /api/teams/{id} - should return 200 when valid")
    void update_success() throws Exception {
        TeamResponse res = new TeamResponse(1L, "Updated", "new desc",
                1L, "Dept One",
                GeneralStatus.ACTIVE, LocalDateTime.now());
        when(teamService.update(eq(1L), any(TeamRequest.class))).thenReturn(res);
        mockMvc.perform(put("/api/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("Updated", "new desc", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated"))
                .andExpect(jsonPath("$.data.description").value("new desc"));
        verify(teamService).update(eq(1L), any(TeamRequest.class));
    }

    @Test
    @DisplayName("PUT /api/teams/{id} - should return 400 when team not found")
    void update_notFound() throws Exception {
        when(teamService.update(eq(99L), any(TeamRequest.class)))
                .thenThrow(new IllegalArgumentException("TEAM_NOT_FOUND"));
        mockMvc.perform(put("/api/teams/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("X", "d", 1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Team not found"));
    }

    @Test
    @DisplayName("GET /api/teams - should return team list")
    void list_success() throws Exception {
        PageResponse<TeamResponse> page = new PageResponse<>(
                List.of(sampleTeamResponse(1L), sampleTeamResponse(2L)),
                0, 10, 2L, 1, false, false);
        when(teamService.list(any(TeamFilterRequest.class), any())).thenReturn(page);
        mockMvc.perform(get("/api/teams").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].teamId").value(1))
                .andExpect(jsonPath("$.data.items[1].teamId").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));
        verify(teamService).list(any(), any());
    }

    @Test
    @DisplayName("GET /api/teams/{id} - should return 200 with detail")
    void detail_success() throws Exception {
        when(teamService.detail(1L)).thenReturn(sampleDetailResponse(1L));
        mockMvc.perform(get("/api/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.memberCount").value(5))
                .andExpect(jsonPath("$.data.departmentName").value("Dept One"));
    }

    @Test
    @DisplayName("GET /api/teams/{id} - should return 400 when not found")
    void detail_notFound() throws Exception {
        when(teamService.detail(99L)).thenThrow(new IllegalArgumentException("TEAM_NOT_FOUND"));
        mockMvc.perform(get("/api/teams/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Team not found"));
    }

    @Test
    @DisplayName("DELETE /api/teams/{id} - should return 200 with message")
    void delete_success() throws Exception {
        doNothing().when(teamService).delete(1L);
        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Delete success"));
        verify(teamService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/teams/{id} - should return 400 when not active")
    void delete_notActive() throws Exception {
        doThrow(new IllegalArgumentException("TEAM_NOT_ACTIVE")).when(teamService).delete(1L);
        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/teams/{id}/managers/{userId} - should return 200")
    void addManager_success() throws Exception {
        doNothing().when(teamService).addManager(1L, 5L);
        mockMvc.perform(post("/api/teams/1/managers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(teamService).addManager(1L, 5L);
    }

    @Test
    @DisplayName("DELETE /api/teams/{id}/managers/{userId} - should return 200")
    void removeManager_success() throws Exception {
        doNothing().when(teamService).removeManager(1L, 5L);
        mockMvc.perform(delete("/api/teams/1/managers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(teamService).removeManager(1L, 5L);
    }
}