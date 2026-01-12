package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private TeamService teamService;

    private static User user(Long id, String email, String fullName) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        u.setFullName(fullName);
        return u;
    }

    private static Team team(Long teamId, String name, String description, User manager, Department department) {
        Team t = new Team();
        t.setTeamId(teamId);
        t.setName(name);
        t.setDescription(description);
        t.setManager(manager);
        t.setDepartment(department);
        return t;
    }
    
    private static Department department(Long departmentId, String description, String name) {
        Department d = new Department();
        d.setDepartmentId(departmentId);
        d.setDescription(description);
        d.setName(name);
        return d;
    }

    @Test
    @DisplayName("createTeam() should create team and return TeamResponse")
    void createTeam_shouldCreateAndReturnResponse() {
        TeamRequest req = new TeamRequest("CNTT", "desc", 1L, 1L);
        User manager = user(1L, "m1@example.com", "Manager One");
        Department department = department(1L, "Department One", "Department One");

        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team saved = inv.getArgument(0);
            saved.setTeamId(1L);
            return saved;
        });

        TeamResponse res = teamService.createTeam(req);

        assertEquals(1L, res.getTeamId());
        assertEquals("CNTT", res.getName());
        assertEquals("desc", res.getDescription());
        assertNotNull(res.getManager());
        assertEquals(1L, res.getManager().getUserId());
        assertEquals("m1@example.com", res.getManager().getEmail());
        assertEquals("Manager One", res.getManager().getFullName());
        assertNotNull(res.getDepartment());
        assertEquals(1L, res.getDepartment().getDepartmentId());
        assertEquals("Department One", res.getDepartment().getName());
        assertEquals("Department One", res.getDepartment().getDescription());

        verify(userRepository).findById(1L);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("createTeam() should throw when manager not found")
    void createTeam_shouldThrow_whenManagerNotFound() {
        TeamRequest req = new TeamRequest("CNTT", "desc", 1L, 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> teamService.createTeam(req));
        assertEquals("MANAGER_NOT_FOUND", ex.getMessage());

        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTeam() should update team and return TeamResponse")
    void updateTeam_shouldUpdateAndReturnResponse() {
        Long teamId = 1L;

        User oldManager = user(1L, "old@example.com", "Old Manager");
        Department oldDepartment = department(1L, "Old Department", "Old Department");
        Team existing = team(teamId, "Old", "Old desc", oldManager, oldDepartment);

        TeamRequest req = new TeamRequest("New Name", "New desc", 2L, 2L);
        User newManager = user(2L, "new@example.com", "New Manager");
        Department newDepartment = department(2L, "New Department", "New Department");

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newManager));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDepartment));

        TeamResponse res = teamService.updateTeam(teamId, req);

        assertEquals(teamId, res.getTeamId());
        assertEquals("New Name", res.getName());
        assertEquals("New desc", res.getDescription());
        assertEquals(2L, res.getManager().getUserId());
        assertEquals("new@example.com", res.getManager().getEmail());
        assertEquals("New Manager", res.getManager().getFullName());

        assertEquals("New Name", existing.getName());
        assertEquals("New desc", existing.getDescription());
        assertEquals(newManager, existing.getManager());

        verify(teamRepository, times(1)).findById(teamId);
        verify(userRepository, times(1)).findById(2L);
        verify(departmentRepository, times(1)).findById(2L);
        verify(teamRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("updateTeam() should throw when team not found")
    void updateTeam_shouldThrow_whenTeamNotFound() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(1L, new TeamRequest("CNTT", "desc", 1L, 1L)));
        assertEquals("TEAM_NOT_FOUND", ex.getMessage());

        verify(userRepository, never()).findById(any());
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllTeams() should map teams to TeamResponse list")
    void getAllTeams_shouldReturnMappedList() {
        Pageable pageable = PageRequest.of(0, 10);

        Team t1 = team(1L, "T1", "D1", user(1L, "m1@example.com", "M1"), department(1L, "D1", "D1"));
        Team t2 = team(2L, "T2", "D2", user(2L, "m2@example.com", "M2"), department(2L, "D2", "D2"));

        Page<Team> page = new PageImpl<>(List.of(t1, t2), pageable, 2);

        when(teamRepository.findAll(pageable)).thenReturn(page);

        PageResponse<TeamResponse> res = teamService.getAllTeams(pageable);

        assertNotNull(res);
        assertEquals(2, res.getItems().size());

        assertEquals(1L, res.getItems().get(0).getTeamId());
        assertEquals("T1", res.getItems().get(0).getName());
        assertEquals("D1", res.getItems().get(0).getDescription());
        assertEquals(1L, res.getItems().get(0).getManager().getUserId());

        assertEquals(2L, res.getItems().get(1).getTeamId());
        assertEquals("T2", res.getItems().get(1).getName());
        assertEquals("D2", res.getItems().get(1).getDescription());
        assertEquals(2L, res.getItems().get(1).getManager().getUserId());

        assertEquals(2L, res.getTotalElements());
        assertEquals(1, res.getTotalPages());
        assertEquals(0, res.getPage());
        assertEquals(10, res.getSize());
    }

    @Test
    @DisplayName("getTeamById() should return TeamResponse when found")
    void getTeamById_shouldReturnResponse() {
        Team t = team(1L, "T1", "D1", user(1L, "m1@example.com", "M1"), department(1L, "D1", "D1"));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));

        TeamResponse res = teamService.getTeamById(1L);

        assertEquals(1L, res.getTeamId());
        assertEquals("T1", res.getName());
        assertEquals("D1", res.getDescription());
        assertEquals(1L, res.getManager().getUserId());
    }

    @Test
    @DisplayName("deleteTeam() should delete members then delete team")
    void deleteTeam_shouldDeleteMembersThenTeam() {
        Team t = team(1L, "T1", "D1", user(1L, "m1@example.com", "M1"), department(1L, "D1", "D1"));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));

        teamService.deleteTeam(1L);

        verify(teamMemberRepository).deleteAllByTeam(t);
        verify(teamRepository).delete(t);
    }
}
