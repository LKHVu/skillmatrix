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
import com.das.skillmatrix.dto.request.AddMemberByTeamRequest;
import com.das.skillmatrix.dto.request.AddMemberByUserRequest;
import com.das.skillmatrix.dto.request.EditMemberRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamMemberResponse;
import com.das.skillmatrix.service.PermissionService;
import com.das.skillmatrix.service.TeamMemberService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TeamMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TeamMemberService teamMemberService;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static TeamMemberResponse sampleResponse(Long id) {
        return new TeamMemberResponse(id, 8L, "member1@skillmatrix.com", "Team Member One",
                1L, "Backend Team", 1L, "Backend Developer",
                LocalDateTime.of(2025, 6, 1, 10, 0));
    }

    // ==================== ADD BY USER ====================

    @Test
    @DisplayName("POST /api/team-members/by-user - should return 200 when valid")
    void addByUser_success() throws Exception {
        when(teamMemberService.addByUser(any(AddMemberByUserRequest.class)))
                .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        String body = """
                {
                    "userId": 8,
                    "assignments": [
                        {"teamId": 1, "positionId": 1},
                        {"teamId": 2, "positionId": 2}
                    ]
                }
                """;
        mockMvc.perform(post("/api/team-members/by-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[0].email").value("member1@skillmatrix.com"));
        verify(teamMemberService).addByUser(any(AddMemberByUserRequest.class));
    }

    @Test
    @DisplayName("POST /api/team-members/by-user - should return 400 when userId is null")
    void addByUser_validation() throws Exception {
        mockMvc.perform(post("/api/team-members/by-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignments": [{"teamId": 1, "positionId": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value(400));
        verify(teamMemberService, never()).addByUser(any());
    }

    @Test
    @DisplayName("POST /api/team-members/by-user - should return 400 when assignments empty")
    void addByUser_emptyAssignments() throws Exception {
        mockMvc.perform(post("/api/team-members/by-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 8, "assignments": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verify(teamMemberService, never()).addByUser(any());
    }

    @Test
    @DisplayName("POST /api/team-members/by-user - should return 400 when user not in same career")
    void addByUser_notSameCareer() throws Exception {
        when(teamMemberService.addByUser(any(AddMemberByUserRequest.class)))
                .thenThrow(new IllegalArgumentException("USER_NOT_IN_SAME_CAREER"));

        String body = """
                {
                    "userId": 16,
                    "assignments": [{"teamId": 1, "positionId": 1}]
                }
                """;
        mockMvc.perform(post("/api/team-members/by-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("User does not belong to this career"));
    }

    // ==================== ADD BY TEAM ====================

    @Test
    @DisplayName("POST /api/team-members/by-team - should return 200 when valid")
    void addByTeam_success() throws Exception {
        when(teamMemberService.addByTeam(any(AddMemberByTeamRequest.class)))
                .thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/team-members/by-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMemberByTeamRequest(1L, "member1@skillmatrix.com", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("member1@skillmatrix.com"))
                .andExpect(jsonPath("$.data.teamName").value("Backend Team"));
        verify(teamMemberService).addByTeam(any(AddMemberByTeamRequest.class));
    }

    @Test
    @DisplayName("POST /api/team-members/by-team - should return 400 when email invalid")
    void addByTeam_invalidEmail() throws Exception {
        mockMvc.perform(post("/api/team-members/by-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId": 1, "email": "not-an-email", "positionId": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verify(teamMemberService, never()).addByTeam(any());
    }

    @Test
    @DisplayName("POST /api/team-members/by-team - should return 409 when duplicate")
    void addByTeam_duplicate() throws Exception {
        when(teamMemberService.addByTeam(any(AddMemberByTeamRequest.class)))
                .thenThrow(new IllegalArgumentException("USER_ALREADY_IN_TEAM"));

        mockMvc.perform(post("/api/team-members/by-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMemberByTeamRequest(1L, "member1@skillmatrix.com", 1L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("User already exists in this team"));
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("PUT /api/team-members/{id} - should return 200 when valid")
    void update_success() throws Exception {
        TeamMemberResponse res = sampleResponse(1L);
        when(teamMemberService.update(eq(1L), any(EditMemberRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/team-members/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditMemberRequest(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.positionId").value(1));
        verify(teamMemberService).update(eq(1L), any(EditMemberRequest.class));
    }

    @Test
    @DisplayName("PUT /api/team-members/{id} - should return 400 when positionId is null")
    void update_validation() throws Exception {
        mockMvc.perform(put("/api/team-members/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verify(teamMemberService, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("PUT /api/team-members/{id} - should return 404 when member not found")
    void update_notFound() throws Exception {
        when(teamMemberService.update(eq(99L), any(EditMemberRequest.class)))
                .thenThrow(new IllegalArgumentException("TEAM_MEMBER_NOT_FOUND"));

        mockMvc.perform(put("/api/team-members/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditMemberRequest(1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Team member not found"));
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("DELETE /api/team-members/{id} - should return 200")
    void delete_success() throws Exception {
        doNothing().when(teamMemberService).delete(1L);
        mockMvc.perform(delete("/api/team-members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Delete success"));
        verify(teamMemberService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/team-members/{id} - should return 404 when not found")
    void delete_notFound() throws Exception {
        doThrow(new IllegalArgumentException("TEAM_MEMBER_NOT_FOUND"))
                .when(teamMemberService).delete(99L);
        mockMvc.perform(delete("/api/team-members/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Team member not found"));
    }

    @Test
    @DisplayName("DELETE /api/team-members/{id} - should return 400 when team not active")
    void delete_teamNotActive() throws Exception {
        doThrow(new IllegalArgumentException("TEAM_NOT_ACTIVE"))
                .when(teamMemberService).delete(1L);
        mockMvc.perform(delete("/api/team-members/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== LIST ====================

    @Test
    @DisplayName("GET /api/team-members?teamId=1 - should return paginated list")
    void listByTeam_success() throws Exception {
        PageResponse<TeamMemberResponse> page = new PageResponse<>(
                List.of(sampleResponse(1L), sampleResponse(2L)),
                0, 10, 2L, 1, false, false);
        when(teamMemberService.listByTeam(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/team-members")
                        .param("teamId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[1].id").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1));
        verify(teamMemberService).listByTeam(eq(1L), any());
    }

    @Test
    @DisplayName("GET /api/team-members - should return 400 when teamId missing")
    void listByTeam_missingTeamId() throws Exception {
        mockMvc.perform(get("/api/team-members"))
                .andExpect(status().isBadRequest());
    }
}
