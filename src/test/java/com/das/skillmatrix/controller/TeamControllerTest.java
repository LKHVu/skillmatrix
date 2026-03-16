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
import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.request.criteria.TeamSearchCriteria;
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
                GeneralStatus.ACTIVE, LocalDateTime.of(2025, 6, 1, 10, 0),
                new TeamResponse.Department(1L, "Dept One"));
    }
    private static TeamDetailResponse sampleDetailResponse(long teamId) {
        return new TeamDetailResponse(teamId, "Team Alpha", "desc",
                GeneralStatus.ACTIVE, LocalDateTime.of(2025, 6, 1, 10, 0), 5L,
                new TeamDetailResponse.Department(1L, "Dept One"));
    }

    @Test
    @DisplayName("POST /api/teams - should return 201 when valid")
    void createTeam_success() throws Exception {
        when(teamService.createTeam(any(TeamRequest.class))).thenReturn(sampleTeamResponse(1L));
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("Team Alpha", "desc", 1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.department.departmentId").value(1))
                .andExpect(jsonPath("$.data.department.name").value("Dept One"));
        verify(teamService).createTeam(any(TeamRequest.class));
    }
    @Test
    @DisplayName("POST /api/teams - should return 400 when validation fails")
    void createTeam_validation() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":"desc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value(400));
        verify(teamService, never()).createTeam(any());
    }
    @Test
    @DisplayName("POST /api/teams - should return 404 when department not found")
    void createTeam_departmentNotFound() throws Exception {
        when(teamService.createTeam(any(TeamRequest.class)))
                .thenThrow(new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("Team", "desc", 99L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/teams/{id} - should return 200 when valid")
    void updateTeam_success() throws Exception {
        TeamResponse res = new TeamResponse(1L, "Updated", "new desc",
                GeneralStatus.ACTIVE, LocalDateTime.now(),
                new TeamResponse.Department(1L, "Dept One"));
        when(teamService.updateTeam(eq(1L), any(TeamRequest.class))).thenReturn(res);
        mockMvc.perform(put("/api/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("Updated", "new desc", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated"))
                .andExpect(jsonPath("$.data.description").value("new desc"));
        verify(teamService).updateTeam(eq(1L), any(TeamRequest.class));
    }
    @Test
    @DisplayName("PUT /api/teams/{id} - should return 404 when team not found")
    void updateTeam_notFound() throws Exception {
        when(teamService.updateTeam(eq(99L), any(TeamRequest.class)))
                .thenThrow(new ResourceNotFoundException("TEAM_NOT_FOUND"));
        mockMvc.perform(put("/api/teams/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TeamRequest("X", "d", 1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("TEAM_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/teams - should return global search when no departmentId")
    void getAllTeams_globalSearch() throws Exception {
        PageResponse<TeamResponse> page = new PageResponse<>(
                List.of(sampleTeamResponse(1L), sampleTeamResponse(2L)),
                0, 10, 2L, 1, false, false);
        when(teamService.getAllTeams(any(TeamSearchCriteria.class), any())).thenReturn(page);
        mockMvc.perform(get("/api/teams").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].teamId").value(1))
                .andExpect(jsonPath("$.data.items[1].teamId").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));
        verify(teamService).getAllTeams(any(), any());
        verify(teamService, never()).getTeamsByDepartment(any(), any());
    }
    @Test
    @DisplayName("GET /api/teams?departmentId=1 - should delegate to getTeamsByDepartment")
    void getAllTeams_departmentSearch() throws Exception {
        PageResponse<TeamResponse> page = new PageResponse<>(
                List.of(sampleTeamResponse(1L)), 0, 10, 1L, 1, false, false);
        when(teamService.getTeamsByDepartment(any(TeamSearchCriteria.class), any())).thenReturn(page);
        mockMvc.perform(get("/api/teams").param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].teamId").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        verify(teamService).getTeamsByDepartment(any(), any());
        verify(teamService, never()).getAllTeams(any(), any());
    }

    @Test
    @DisplayName("GET /api/teams/{id} - should return 200 with detail")
    void getTeamById_success() throws Exception {
        when(teamService.getTeamById(1L)).thenReturn(sampleDetailResponse(1L));
        mockMvc.perform(get("/api/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.memberCount").value(5))
                .andExpect(jsonPath("$.data.department.name").value("Dept One"));
    }
    @Test
    @DisplayName("GET /api/teams/{id} - should return 404 when not found")
    void getTeamById_notFound() throws Exception {
        when(teamService.getTeamById(99L)).thenThrow(new ResourceNotFoundException("TEAM_NOT_FOUND"));
        mockMvc.perform(get("/api/teams/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("TEAM_NOT_FOUND"))
                .andExpect(jsonPath("$.error.errorCode").value(404));
    }

    @Test
    @DisplayName("DELETE /api/teams/{id} - should return 200")
    void deleteTeam_success() throws Exception {
        doNothing().when(teamService).deleteTeam(1L);
        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(teamService).deleteTeam(1L);
    }
    @Test
    @DisplayName("DELETE /api/teams/{id} - should return 400 when not active")
    void deleteTeam_notActive() throws Exception {
        doThrow(new IllegalArgumentException("TEAM_NOT_ACTIVE")).when(teamService).deleteTeam(1L);
        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/teams/{id}/managers - should return 200")
    void assignManagers_success() throws Exception {
        doNothing().when(teamService).assignManagers(eq(1L), anyList());
        mockMvc.perform(post("/api/teams/1/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(teamService).assignManagers(eq(1L), eq(List.of(1L, 2L, 3L)));
    }
}