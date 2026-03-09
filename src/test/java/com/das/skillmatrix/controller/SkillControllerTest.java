package com.das.skillmatrix.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.das.skillmatrix.dto.request.SkillRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.SkillResponse;
import com.das.skillmatrix.entity.SkillStatus;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.service.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SkillControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillService skillService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static SkillResponse skillResponse(long id) {
        return new SkillResponse(id, "Java", "desc", SkillStatus.ACTIVE);
    }

    @Test
    @DisplayName("POST /api/skills should return 201 and skill when valid")
    void createSkill_shouldReturnCreated() throws Exception {
        SkillRequest req = new SkillRequest("Java", "desc");
        SkillResponse res = skillResponse(1L);

        when(skillService.createSkill(any(SkillRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skillId").value(1))
                .andExpect(jsonPath("$.data.name").value("Java"))
                .andExpect(jsonPath("$.data.description").value("desc"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/skills should return 400 when request invalid")
    void createSkill_shouldReturnBadRequest_whenInvalid() throws Exception {
        String badJson = """
                {"name":"","description":""}
                """;

        mockMvc.perform(post("/api/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value(400))
                .andExpect(jsonPath("$.data.name").value("Name is required"))
                .andExpect(jsonPath("$.data.description").value("Description is required"));
    }

    @Test
    @DisplayName("PUT /api/skills/{id} should return 200 and updated skill")
    void updateSkill_shouldReturnOk() throws Exception {
        SkillRequest req = new SkillRequest("Java", "new desc");
        SkillResponse res = new SkillResponse(1L, "Java", "new desc", SkillStatus.ACTIVE);

        when(skillService.updateSkill(eq(1L), any(SkillRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/skills/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skillId").value(1))
                .andExpect(jsonPath("$.data.description").value("new desc"));
    }

    @Test
    @DisplayName("GET /api/skills should return 200 and list")
    void getAllSkills_shouldReturnOk() throws Exception {
        PageResponse<SkillResponse> pageResponse = new PageResponse<>(
                List.of(skillResponse(1L), new SkillResponse(2L, "SQL", "d2", SkillStatus.INACTIVE)),
                0,
                10,
                2L,
                1,
                false,
                false);

        when(skillService.listSkills(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/skills")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].skillId").value(1))
                .andExpect(jsonPath("$.data.items[1].skillId").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("GET /api/skills/{id} should return 404 when not found")
    void getSkillById_shouldReturnNotFound_whenServiceThrows() throws Exception {
        when(skillService.getSkillById(1L)).thenThrow(new ResourceNotFoundException("SKILL_NOT_FOUND"));

        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("SKILL_NOT_FOUND"))
                .andExpect(jsonPath("$.error.errorCode").value(404));
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} should return 204")
    void deleteSkill_shouldReturnNoContent() throws Exception {
        doNothing().when(skillService).deleteSkill(1L);

        mockMvc.perform(delete("/api/skills/1"))
                .andExpect(status().isNoContent());

        verify(skillService).deleteSkill(1L);
    }
}
