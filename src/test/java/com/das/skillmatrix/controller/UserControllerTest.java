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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.das.skillmatrix.config.JwtAuthenticationFilter;
import com.das.skillmatrix.dto.request.CreateUserRequest;
import com.das.skillmatrix.dto.request.DeactivateUserRequest;
import com.das.skillmatrix.dto.request.UpdateUserRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.PositionBrief;
import com.das.skillmatrix.dto.response.UserDetailResponse;
import com.das.skillmatrix.dto.response.UserResponse;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.exception.GlobalExceptionHandler;
import com.das.skillmatrix.security.JwtUtil;
import com.das.skillmatrix.service.CustomUserDetailsService;
import com.das.skillmatrix.service.PermissionService;
import com.das.skillmatrix.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    PermissionService permissionService;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    CustomUserDetailsService customUserDetailsService;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private UserResponse userResponse(Long id, String email, GeneralStatus status) {
        UserResponse r = new UserResponse();
        r.setUserId(id);
        r.setEmail(email);
        r.setFullName("User " + id);
        r.setRole("STAFF");
        r.setStatus(status);
        r.setPositions(List.of(new PositionBrief(1L, "Dev")));
        r.setCreatedAt(NOW);
        return r;
    }

    // ===================== LIST =====================

    @Test
    @DisplayName("GET /api/users should return 200 and paged list")
    void list_shouldReturnPagedList() throws Exception {
        List<UserResponse> items = List.of(
                userResponse(1L, "u1@test.com", GeneralStatus.ACTIVE),
                userResponse(2L, "u2@test.com", GeneralStatus.DEACTIVE));

        PageResponse<UserResponse> page = new PageResponse<>(items, 0, 10, 2L, 1, false, false);
        when(userService.list(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].email").value("u1@test.com"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[1].status").value("DEACTIVE"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    // ===================== LIST BY TEAM =====================

    @Test
    @DisplayName("GET /api/users/by-team/{teamId} should return 200 and paged list")
    void listByTeam_shouldReturnPagedList() throws Exception {
        List<UserResponse> items = List.of(userResponse(1L, "u@test.com", GeneralStatus.ACTIVE));
        PageResponse<UserResponse> page = new PageResponse<>(items, 0, 5, 1L, 1, false, false);

        when(permissionService.checkTeamViewAccess(1L)).thenReturn(true);
        when(userService.listByTeam(eq(1L), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/by-team/1?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].email").value("u@test.com"));
    }

    // ===================== CREATE =====================

    @Test
    @DisplayName("POST /api/users should return 201 and created user")
    void create_shouldReturnCreatedUser() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("new@test.com");
        req.setRole("STAFF");
        req.setPositionIds(List.of(1L));
        req.setTeamId(1L);

        UserResponse resp = userResponse(10L, "new@test.com", GeneralStatus.ACTIVE);
        when(userService.create(any(CreateUserRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.email").value("new@test.com"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 when email is blank")
    void create_shouldReturn400_whenInvalidInput() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("");
        req.setRole("STAFF");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===================== UPDATE =====================

    @Test
    @DisplayName("PUT /api/users/{id} should return 200 and updated user")
    void update_shouldReturnUpdatedUser() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("updated@test.com");
        req.setRole("STAFF");
        req.setPositionIds(List.of(1L));

        UserResponse resp = userResponse(1L, "updated@test.com", GeneralStatus.ACTIVE);

        when(permissionService.canManageUser(1L)).thenReturn(true);
        when(userService.update(eq(1L), any(UpdateUserRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("updated@test.com"));
    }

    // ===================== DEACTIVATE / DELETE =====================

    @Test
    @DisplayName("POST /api/users/{id}/deactivate-or-delete should return 200")
    void deactivateOrDelete_shouldReturn200() throws Exception {
        DeactivateUserRequest req = new DeactivateUserRequest();
        req.setAction("DEACTIVE");
        req.setDeactiveType("UNLIMITED");

        when(permissionService.canManageUser(1L)).thenReturn(true);
        doNothing().when(userService).deactivateOrDelete(eq(1L), any(DeactivateUserRequest.class));

        mockMvc.perform(post("/api/users/1/deactivate-or-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ===================== REACTIVATE =====================

    @Test
    @DisplayName("POST /api/users/{id}/reactivate should return 200")
    void reactivate_shouldReturn200() throws Exception {
        UserResponse resp = userResponse(1L, "u@test.com", GeneralStatus.ACTIVE);

        when(permissionService.canManageUser(1L)).thenReturn(true);
        when(userService.reactivate(1L)).thenReturn(resp);

        mockMvc.perform(post("/api/users/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // ===================== DETAIL =====================

    @Test
    @DisplayName("GET /api/users/{id} should return 200 and detail")
    void detail_shouldReturnDetail() throws Exception {
        UserDetailResponse resp = new UserDetailResponse();
        resp.setUserId(1L);
        resp.setEmail("u@test.com");
        resp.setFullName("Test User");
        resp.setRole("STAFF");
        resp.setStatus(GeneralStatus.ACTIVE);
        resp.setPositions(List.of(new PositionBrief(1L, "Dev")));
        resp.setCreatedAt(NOW);

        when(permissionService.checkUserViewAccess(1L)).thenReturn(true);
        when(userService.getDetail(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("u@test.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when not found")
    void detail_shouldReturn404_whenNotFound() throws Exception {
        when(permissionService.checkUserViewAccess(999L)).thenReturn(true);
        when(userService.getDetail(999L)).thenThrow(new IllegalArgumentException("USER_NOT_FOUND"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
