package com.das.skillmatrix.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
        return new TeamResponse(
                teamId,
                "Team Alpha",
                "A great team",
                GeneralStatus.ACTIVE,
                LocalDateTime.of(2025, 6, 1, 10, 0),
                new TeamResponse.Department(1L, "Dept One"));
    }

    private static TeamDetailResponse sampleTeamDetailResponse(long teamId) {
        return new TeamDetailResponse(
                teamId,
                "Team Alpha",
                "A great team",
                GeneralStatus.ACTIVE,
                LocalDateTime.of(2025, 6, 1, 10, 0),
                5L,
                new TeamDetailResponse.Department(1L, "Dept One"));
    }

    @Nested
    @DisplayName("POST /api/teams")
    class CreateTeam {
        @Test
        @DisplayName("should return 201 and team when valid")
        void success() throws Exception {
            TeamRequest req = new TeamRequest("Team Alpha", "A great team", 1L);
            TeamResponse res = sampleTeamResponse(1L);
            when(teamService.createTeam(any(TeamRequest.class))).thenReturn(res);
            mockMvc.perform(post("/api/teams")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.teamId").value(1))
                    .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                    .andExpect(jsonPath("$.data.description").value("A great team"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.department.departmentId").value(1))
                    .andExpect(jsonPath("$.data.department.name").value("Dept One"));
            verify(teamService).createTeam(any(TeamRequest.class));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void blankName() throws Exception {
            String json = """
                    {"name":"","description":"desc","departmentId":1}
                    """;
            mockMvc.perform(post("/api/teams")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.errorCode").value(400));
            verify(teamService, never()).createTeam(any());
        }

        @Test
        @DisplayName("should return 400 when departmentId is null")
        void nullDepartmentId() throws Exception {
            String json = """
                    {"name":"Team","description":"desc"}
                    """;
            mockMvc.perform(post("/api/teams")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
            verify(teamService, never()).createTeam(any());
        }

        @Test
        @DisplayName("should return 404 when department not found")
        void departmentNotFound() throws Exception {
            TeamRequest req = new TeamRequest("Team", "desc", 99L);
            when(teamService.createTeam(any(TeamRequest.class)))
                    .thenThrow(new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
            mockMvc.perform(post("/api/teams")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.message").value("DEPARTMENT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("PUT /api/teams/{teamId}")
    class UpdateTeam {
        @Test
        @DisplayName("should return 200 and updated team")
        void success() throws Exception {
            TeamRequest req = new TeamRequest("Updated", "new desc", 1L);
            TeamResponse res = new TeamResponse(1L, "Updated", "new desc",
                    GeneralStatus.ACTIVE, LocalDateTime.now(),
                    new TeamResponse.Department(1L, "Dept One"));
            when(teamService.updateTeam(eq(1L), any(TeamRequest.class))).thenReturn(res);
            mockMvc.perform(put("/api/teams/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.teamId").value(1))
                    .andExpect(jsonPath("$.data.name").value("Updated"))
                    .andExpect(jsonPath("$.data.description").value("new desc"));
            verify(teamService).updateTeam(eq(1L), any(TeamRequest.class));
        }

        @Test
        @DisplayName("should return 404 when team not found")
        void teamNotFound() throws Exception {
            TeamRequest req = new TeamRequest("X", "d", 1L);
            when(teamService.updateTeam(eq(99L), any(TeamRequest.class)))
                    .thenThrow(new ResourceNotFoundException("TEAM_NOT_FOUND"));
            mockMvc.perform(put("/api/teams/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value("TEAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void blankName() throws Exception {
            String json = """
                    {"name":"","description":"desc","departmentId":1}
                    """;
            mockMvc.perform(put("/api/teams/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/teams")
    class GetAllTeams {
        @Test
        @DisplayName("should return 200 and page of teams (global search)")
        void globalSearch() throws Exception {
            PageResponse<TeamResponse> pageResponse = new PageResponse<>(
                    List.of(sampleTeamResponse(1L), sampleTeamResponse(2L)),
                    0, 10, 2L, 1, false, false);
            when(teamService.getAllTeams(any(TeamSearchCriteria.class), any()))
                    .thenReturn(pageResponse);
            mockMvc.perform(get("/api/teams")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].teamId").value(1))
                    .andExpect(jsonPath("$.data.items[1].teamId").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10));
            verify(teamService).getAllTeams(any(TeamSearchCriteria.class), any());
            verify(teamService, never()).getTeamsByDepartment(any(), any());
        }

        @Test
        @DisplayName("should delegate to getTeamsByDepartment when departmentId is present")
        void departmentSearch() throws Exception {
            PageResponse<TeamResponse> pageResponse = new PageResponse<>(
                    List.of(sampleTeamResponse(1L)),
                    0, 10, 1L, 1, false, false);
            when(teamService.getTeamsByDepartment(any(TeamSearchCriteria.class), any()))
                    .thenReturn(pageResponse);
            mockMvc.perform(get("/api/teams")
                    .param("departmentId", "1")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items[0].teamId").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
            verify(teamService).getTeamsByDepartment(any(TeamSearchCriteria.class), any());
            verify(teamService, never()).getAllTeams(any(), any());
        }

        @Test
        @DisplayName("should pass keyword and status as search criteria")
        void withSearchCriteria() throws Exception {
            PageResponse<TeamResponse> pageResponse = new PageResponse<>(
                    List.of(), 0, 10, 0L, 0, false, false);
            when(teamService.getAllTeams(any(TeamSearchCriteria.class), any()))
                    .thenReturn(pageResponse);
            mockMvc.perform(get("/api/teams")
                    .param("keyword", "alpha")
                    .param("status", "ACTIVE")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}")
    class GetTeamById {
        @Test
        @DisplayName("should return 200 and team detail")
        void success() throws Exception {
            TeamDetailResponse res = sampleTeamDetailResponse(1L);
            when(teamService.getTeamById(1L)).thenReturn(res);
            mockMvc.perform(get("/api/teams/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.teamId").value(1))
                    .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                    .andExpect(jsonPath("$.data.memberCount").value(5))
                    .andExpect(jsonPath("$.data.department.departmentId").value(1))
                    .andExpect(jsonPath("$.data.department.name").value("Dept One"));
        }

        @Test
        @DisplayName("should return 404 when team not found")
        void notFound() throws Exception {
            when(teamService.getTeamById(99L))
                    .thenThrow(new ResourceNotFoundException("TEAM_NOT_FOUND"));
            mockMvc.perform(get("/api/teams/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.message").value("TEAM_NOT_FOUND"))
                    .andExpect(jsonPath("$.error.errorCode").value(404));
        }
    }

    @Nested
    @DisplayName("DELETE /api/teams/{teamId}")
    class DeleteTeam {
        @Test
        @DisplayName("should return 200 with deactivation message when team has members")
        void deactivate() throws Exception {
            when(teamService.deleteTeam(1L))
                    .thenReturn("Team has been deactivated successfully.");
            mockMvc.perform(delete("/api/teams/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("Team has been deactivated successfully."));
            verify(teamService).deleteTeam(1L);
        }

        @Test
        @DisplayName("should return 200 with deletion message when team has no members")
        void softDelete() throws Exception {
            when(teamService.deleteTeam(2L))
                    .thenReturn("Team deleted successfully.");
            mockMvc.perform(delete("/api/teams/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("Team deleted successfully."));
        }

        @Test
        @DisplayName("should return 404 when team not found")
        void notFound() throws Exception {
            when(teamService.deleteTeam(99L))
                    .thenThrow(new ResourceNotFoundException("TEAM_NOT_FOUND"));
            mockMvc.perform(delete("/api/teams/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value("TEAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when team is not active")
        void notActive() throws Exception {
            when(teamService.deleteTeam(1L))
                    .thenThrow(new IllegalArgumentException("TEAM_NOT_ACTIVE"));
            mockMvc.perform(delete("/api/teams/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/teams/{teamId}/managers")
    class AssignManagers {
        @Test
        @DisplayName("should return 200 when managers assigned")
        void success() throws Exception {
            doNothing().when(teamService).assignManagers(eq(1L), anyList());
            mockMvc.perform(post("/api/teams/1/managers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[1, 2, 3]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            verify(teamService).assignManagers(eq(1L), eq(List.of(1L, 2L, 3L)));
        }

        @Test
        @DisplayName("should return 400 when team not found")
        void teamNotFound() throws Exception {
            doThrow(new IllegalArgumentException("TEAM_NOT_FOUND"))
                    .when(teamService).assignManagers(eq(99L), anyList());
            mockMvc.perform(post("/api/teams/99/managers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[1]"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}