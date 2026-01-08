package com.das.skillmatrix.controller;

import com.das.skillmatrix.config.JwtAuthenticationFilter;
import com.das.skillmatrix.dto.request.CareerRequest;
import com.das.skillmatrix.dto.response.CareerDeleteCheckResponse;
import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.dto.response.DepartmentBrief;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.exception.GlobalExceptionHandler;
import com.das.skillmatrix.security.JwtUtil;
import com.das.skillmatrix.service.CareerService;
import com.das.skillmatrix.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CareerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class CareerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CareerService careerService;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/careers should return 200 and created career")
    void create_shouldReturnCareer_whenValid() throws Exception {
        CareerRequest req = new CareerRequest();
        req.setName("IT");
        req.setDescription("Information Technology");

        CareerResponse resp = new CareerResponse(1L, "IT", "Information Technology", 0);

        when(careerService.create(any(CareerRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/careers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.careerId").value(1))
                .andExpect(jsonPath("$.data.name").value("IT"));
    }

    @Test
    @DisplayName("POST /api/careers should return 400 when name is blank")
    void create_shouldReturn400_whenInvalidInput() throws Exception {
        CareerRequest req = new CareerRequest();
        req.setName("");
        req.setDescription("x");

        mockMvc.perform(post("/api/careers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/careers/{id} should return 200 and updated career")
    void update_shouldReturnCareer_whenValid() throws Exception {
        CareerRequest req = new CareerRequest();
        req.setName("IT Updated");
        req.setDescription("Desc");

        CareerResponse resp = new CareerResponse(1L, "IT Updated", "Desc", 2);

        when(careerService.update(eq(1L), any(CareerRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/careers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("IT Updated"))
                .andExpect(jsonPath("$.data.departmentsCount").value(2));
    }

    @Test
    @DisplayName("GET /api/careers should return 200 and paged list")
    void list_shouldReturnPagedList() throws Exception {
        List<CareerResponse> items = List.of(
                new CareerResponse(1L, "IT", "Desc", 1),
                new CareerResponse(2L, "Banking", "Desc", 0)
        );

        PageResponse<CareerResponse> page = new PageResponse<>(
                items,
                0,
                20,
                2L,
                1,
                false,
                false
        );

        when(careerService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/careers?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("IT"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/careers/{id} should return 200 and detail")
    void detail_shouldReturnCareer() throws Exception {
        CareerResponse resp = new CareerResponse(1L, "IT", "Desc", 3);
        when(careerService.detail(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/careers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.careerId").value(1))
                .andExpect(jsonPath("$.data.departmentsCount").value(3));
    }

    @Test
    @DisplayName("GET /api/careers/{id}/delete-check should return 200 and check info")
    void deleteCheck_shouldReturnInfo() throws Exception {
        CareerDeleteCheckResponse resp = new CareerDeleteCheckResponse(
                1L,
                "IT",
                1,
                List.of(new DepartmentBrief(10L, "Dev")),
                2,
                true
        );

        when(careerService.deleteCheck(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/careers/1/delete-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requireConfirm").value(true))
                .andExpect(jsonPath("$.data.departmentsCount").value(1))
                .andExpect(jsonPath("$.data.departments[0].name").value("Dev"));
    }

    @Test
    @DisplayName("DELETE /api/careers/{id}?confirm=false should return 200 when service deletes")
    void delete_shouldReturn200_whenSuccess() throws Exception {
        doNothing().when(careerService).delete(1L, false);

        mockMvc.perform(delete("/api/careers/1?confirm=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Delete success"));
    }

    @Test
    @DisplayName("DELETE /api/careers/{id}?confirm=false should return 409 when confirm required")
    void delete_shouldReturn409_whenConfirmRequired() throws Exception {
        doThrow(new IllegalStateException("CAREER_DELETE_CONFIRM_REQUIRED"))
                .when(careerService).delete(1L, false);

        mockMvc.perform(delete("/api/careers/1?confirm=false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value(409));
    }
}