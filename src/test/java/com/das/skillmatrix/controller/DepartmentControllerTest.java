package com.das.skillmatrix.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.das.skillmatrix.dto.request.DepartmentRequest;
import com.das.skillmatrix.dto.response.DepartmentDetailResponse;
import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamBrief;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.exception.GlobalExceptionHandler;
import com.das.skillmatrix.security.JwtUtil;
import com.das.skillmatrix.service.CustomUserDetailsService;
import com.das.skillmatrix.service.DepartmentService;
import com.das.skillmatrix.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class DepartmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    DepartmentService departmentService;

    @MockBean
    PermissionService permissionService;

    // Security beans
    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/departments should return 200 and created department")
    void create_shouldReturnDepartment_whenValid() throws Exception {
        DepartmentRequest req = new DepartmentRequest();
        req.setName("DEV");
        req.setDescription("Developer");
        req.setCareerId(1L);

        DepartmentResponse resp = new DepartmentResponse(1L, "DEV", "Developer", 1L, GeneralStatus.ACTIVE);

        when(permissionService.checkCareerAccess(1L)).thenReturn(true);
        when(departmentService.create(any(DepartmentRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(1))
                .andExpect(jsonPath("$.data.name").value("DEV"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/departments/{id} should return 200 and updated department")
    void update_shouldReturnDepartment_whenValid() throws Exception {
        DepartmentRequest req = new DepartmentRequest();
        req.setName("DEV Updated");
        req.setDescription("Desc");
        req.setCareerId(1L);

        DepartmentResponse resp = new DepartmentResponse(1L, "DEV Updated", "Desc", 1L, GeneralStatus.ACTIVE);

        when(permissionService.checkDepartmentAccess(1L)).thenReturn(true);
        when(departmentService.update(eq(1L), any(DepartmentRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("DEV Updated"));
    }

    @Test
    @DisplayName("GET /api/departments should return 200 and paged list")
    void list_shouldReturnPagedList() throws Exception {
        List<DepartmentResponse> items = List.of(
                new DepartmentResponse(1L, "DEV", "Desc", 1L, GeneralStatus.ACTIVE));

        PageResponse<DepartmentResponse> page = new PageResponse<>(
                items, 0, 10, 1L, 1, false, false);

        when(permissionService.canViewDepartmentList(1L)).thenReturn(true);
        when(departmentService.listByCareer(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/departments?careerId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("DEV"));
    }

    @Test
    @DisplayName("GET /api/departments/{id} should return 200 and detail")
    void detail_shouldReturnDetail() throws Exception {
        DepartmentDetailResponse resp = new DepartmentDetailResponse(
                1L, "DEV", "Desc", 1L, GeneralStatus.ACTIVE, 1,
                List.of(new TeamBrief(100L, "Team1", GeneralStatus.ACTIVE)));

        when(permissionService.canViewDepartmentDetail(1L)).thenReturn(true);
        when(departmentService.detail(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(1))
                .andExpect(jsonPath("$.data.teams[0].name").value("Team1"));
    }

    @Test
    @DisplayName("DELETE /api/departments/{id} should return 200")
    void delete_shouldReturnSuccess() throws Exception {
        when(permissionService.canManageDepartment(1L)).thenReturn(true);
        doNothing().when(departmentService).delete(1L);

        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Delete success"));
    }
}
