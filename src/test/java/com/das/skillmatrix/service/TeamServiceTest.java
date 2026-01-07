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

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.exception.ResourceNotFoundException;
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

    @InjectMocks
    private TeamService teamService;

    private static User user(Long id, String email, String fullName) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        u.setFullName(fullName);
        return u;
    }

    private static Team team(Long teamId, String name, String description, User manager) {
        Team t = new Team();
        t.setTeamId(teamId);
        t.setName(name);
        t.setDescription(description);
        t.setManager(manager);
        return t;
    }
    
    @Test
    @DisplayName("createTeam() should create team and return TeamResponse")
    void createTeam_shouldCreateAndReturnResponse() {
        TeamRequest req = new TeamRequest("CNTT", "desc", 1L);
        User manager = user(1L, "m1@example.com", "Manager One");

        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
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

        verify(userRepository).findById(1L);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("createTeam() should throw when manager not found")
    void createTeam_shouldThrow_whenManagerNotFound() {
        TeamRequest req = new TeamRequest("CNTT", "desc", 1L);
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
        Team existing = team(teamId, "Old", "Old desc", oldManager);

        TeamRequest req = new TeamRequest("New Name", "New desc", 2L);
        User newManager = user(2L, "new@example.com", "New Manager");

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newManager));

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
        verify(teamRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("updateTeam() should throw when team not found")
    void updateTeam_shouldThrow_whenTeamNotFound() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(1L, new TeamRequest("CNTT", "desc", 1L)));
        assertEquals("TEAM_NOT_FOUND", ex.getMessage());

        verify(userRepository, never()).findById(any());
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllTeams() should map teams to TeamResponse list")
    void getAllTeams_shouldReturnMappedList() {
        Team t1 = team(1L, "T1", "D1", user(1L, "m1@example.com", "M1"));
        Team t2 = team(2L, "T2", "D2", user(2L, "m2@example.com", "M2"));

        when(teamRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TeamResponse> res = teamService.getAllTeams();

        assertEquals(2, res.size());
        assertEquals(1L, res.get(0).getTeamId());
        assertEquals("T1", res.get(0).getName());
        assertEquals(1L, res.get(0).getManager().getUserId());

        assertEquals(2L, res.get(1).getTeamId());
        assertEquals("T2", res.get(1).getName());
        assertEquals(2L, res.get(1).getManager().getUserId());
    }

    @Test
    @DisplayName("getTeamById() should return TeamResponse when found")
    void getTeamById_shouldReturnResponse() {
        Team t = team(1L, "T1", "D1", user(1L, "m1@example.com", "M1"));
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
        Team t = team(1L, "T1", "D1", user(1L, "m1@example.com", "M1"));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));

        teamService.deleteTeam(1L);

        verify(teamMemberRepository).deleteAllByTeam(t);
        verify(teamRepository).delete(t);
    }
}
