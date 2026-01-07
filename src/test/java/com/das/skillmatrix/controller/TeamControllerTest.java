package com.das.skillmatrix.controller;

import com.das.skillmatrix.config.JwtAuthenticationFilter;
import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static TeamResponse teamResponse(long teamId) {
        return new TeamResponse(
                teamId,
                "CNTT",
                "desc",
                new TeamResponse.Manager(1L, "m1@example.com", "Manager One"));
    }

    @Test
    @DisplayName("POST /api/teams should return 201 and team when valid")
    void createTeam_shouldReturnCreated() throws Exception {
        TeamRequest req = new TeamRequest("CNTT", "desc", 1L);
        TeamResponse res = teamResponse(1L);

        when(teamService.createTeam(any(TeamRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1))
                .andExpect(jsonPath("$.data.name").value("CNTT"))
                .andExpect(jsonPath("$.data.manager.userId").value(1))
                .andExpect(jsonPath("$.data.manager.email").value("m1@example.com"))
                .andExpect(jsonPath("$.data.manager.fullName").value("Manager One"));
    }

    @Test
    @DisplayName("POST /api/teams should return 400 when request invalid")
    void createTeam_shouldReturnBadRequest_whenInvalid() throws Exception {
        String badJson = """
                {"name":"","description":"desc","managerId":1}
                """;

        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value(400))
                .andExpect(jsonPath("$.data.name").value("Name is required"));
    }

    @Test
    @DisplayName("PUT /api/teams/{teamId} should return 200 and updated team")
    void updateTeam_shouldReturnOk() throws Exception {
        TeamRequest req = new TeamRequest("CNTT", "desc", 1L);
        TeamResponse res = teamResponse(1L);

        when(teamService.updateTeam(eq(1L), any(TeamRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/teams/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(1));
    }

    @Test
    @DisplayName("GET /api/teams should return 200 and list")
    void getAllTeams_shouldReturnOk() throws Exception {
        when(teamService.getAllTeams()).thenReturn(List.of(teamResponse(1L), teamResponse(2L)));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].teamId").value(1))
                .andExpect(jsonPath("$.data[1].teamId").value(2));
    }

    @Test
    @DisplayName("GET /api/teams/{teamId} should return 404 when not found")
    void getTeamById_shouldReturnNotFound_whenServiceThrows() throws Exception {
        when(teamService.getTeamById(1L)).thenThrow(new ResourceNotFoundException("TEAM_NOT_FOUND"));

        mockMvc.perform(get("/api/teams/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("TEAM_NOT_FOUND"))
                .andExpect(jsonPath("$.error.errorCode").value(404));
    }

    @Test
    @DisplayName("DELETE /api/teams/{teamId} should return 204")
    void deleteTeam_shouldReturnNoContent() throws Exception {
        doNothing().when(teamService).deleteTeam(1L);

        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isNoContent());

        verify(teamService).deleteTeam(1L);
    }
}