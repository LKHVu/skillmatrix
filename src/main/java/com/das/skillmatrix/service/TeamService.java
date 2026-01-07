package com.das.skillmatrix.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

@Service
@Transactional
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository,
            TeamMemberRepository teamMemberRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    // Convert Team to TeamResponse
    private TeamResponse toTeamResponse(Team team, User manager) {
        return new TeamResponse(
                team.getTeamId(),
                team.getName(),
                team.getDescription(),
                new TeamResponse.Manager(manager.getUserId(), manager.getEmail(), manager.getFullName()));
    }

    // Team Management (CRUD)
    public TeamResponse createTeam(TeamRequest teamRequest) {
        User manager = userRepository.findById(teamRequest.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("MANAGER_NOT_FOUND"));
        Team team = new Team();
        team.setName(teamRequest.getName());
        team.setDescription(teamRequest.getDescription());
        team.setManager(manager);
        teamRepository.save(team);

        return toTeamResponse(team, manager);
    }

    public TeamResponse updateTeam(Long teamId, TeamRequest teamRequest) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
        User manager = userRepository.findById(teamRequest.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("MANAGER_NOT_FOUND"));

        team.setName(teamRequest.getName());
        team.setDescription(teamRequest.getDescription());
        team.setManager(manager);
        teamRepository.save(team);

        return toTeamResponse(team, manager);
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(team -> toTeamResponse(team, team.getManager()))
                .toList();
    }

    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
        return toTeamResponse(team, team.getManager());
    }

    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
        teamMemberRepository.deleteAllByTeam(team);
        teamRepository.delete(team);
    }

}