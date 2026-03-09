package com.das.skillmatrix.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.annotation.LogActivity;
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

@Service
@Transactional
public class TeamService {
        private final TeamRepository teamRepository;
        private final TeamMemberRepository teamMemberRepository;
        private final UserRepository userRepository;
        private final DepartmentRepository departmentRepository;
        private final BusinessChangeLogService businessChangeLogService;

        public TeamService(TeamRepository teamRepository, UserRepository userRepository,
                        TeamMemberRepository teamMemberRepository, DepartmentRepository departmentRepository,
                        BusinessChangeLogService businessChangeLogService) {
                this.teamRepository = teamRepository;
                this.teamMemberRepository = teamMemberRepository;
                this.userRepository = userRepository;
                this.departmentRepository = departmentRepository;
                this.businessChangeLogService = businessChangeLogService;
        }

        // Convert Team to TeamResponse
        private TeamResponse toTeamResponse(Team team, User manager, Department department) {
                return new TeamResponse(
                                team.getTeamId(),
                                team.getName(),
                                team.getDescription(),
                                manager != null ? new TeamResponse.Manager(manager.getUserId(), manager.getEmail(),
                                                manager.getFullName()) : null,
                                new TeamResponse.Department(department.getDepartmentId(), department.getName(),
                                                department.getDescription()));
        }

        @LogActivity(action = "CREATE_TEAM", entityType = "TEAM")
        public TeamResponse createTeam(TeamRequest teamRequest) {
                User manager = userRepository.findById(teamRequest.getManagerId())
                                .orElseThrow(() -> new ResourceNotFoundException("MANAGER_NOT_FOUND"));
                Department department = departmentRepository.findById(teamRequest.getDepartmentId())
                                .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
                Team team = new Team();
                team.setName(teamRequest.getName());
                team.setDescription(teamRequest.getDescription());
                team.setManagers(new ArrayList<>(List.of(manager))); // Set as single item list
                team.setDepartment(department);
                teamRepository.save(team);

                return toTeamResponse(team, manager, department);
        }

        @LogActivity(action = "UPDATE_TEAM", entityType = "TEAM")
        public TeamResponse updateTeam(Long teamId, TeamRequest teamRequest) {
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
                User manager = userRepository.findById(teamRequest.getManagerId())
                                .orElseThrow(() -> new ResourceNotFoundException("MANAGER_NOT_FOUND"));
                Department department = departmentRepository.findById(teamRequest.getDepartmentId())
                                .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
                team.setName(teamRequest.getName());
                team.setDescription(teamRequest.getDescription());
                team.setManagers(new ArrayList<>(List.of(manager))); // update manager list
                team.setDepartment(department);
                teamRepository.save(team);

                return toTeamResponse(team, manager, department);
        }

        public PageResponse<TeamResponse> getAllTeams(Pageable pageable) {
                Page<Team> teams = teamRepository.findAll(pageable);
                // Convert Team to TeamResponse
                List<TeamResponse> teamResponses = teams.stream()
                                .map(team -> {
                                        User manager = team.getManagers().isEmpty() ? null : team.getManagers().get(0);
                                        return toTeamResponse(team, manager, team.getDepartment());
                                })
                                .toList();
                return new PageResponse<>(
                                teamResponses,
                                teams.getNumber(),
                                teams.getSize(),
                                teams.getTotalElements(),
                                teams.getTotalPages(),
                                teams.hasNext(),
                                teams.hasPrevious());
        }

        public TeamResponse getTeamById(Long teamId) {
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
                User manager = team.getManagers().isEmpty() ? null : team.getManagers().get(0);
                return toTeamResponse(team, manager, team.getDepartment());
        }

        @LogActivity(action = "DELETE_TEAM", entityType = "TEAM")
        public void deleteTeam(Long teamId) {
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
                teamMemberRepository.deleteAllByTeam(team);
                teamRepository.delete(team);
        }

        @LogActivity(action = "ASSIGN_TEAM_MANAGERS", entityType = "TEAM")
        public void assignManagers(Long teamId, List<Long> managerIds) {
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new IllegalArgumentException("TEAM_NOT_FOUND"));
                String oldManagerIds = team.getManagers().stream()
                                .map(u -> u.getUserId().toString()).toList().toString();

                List<User> managers = userRepository.findAllById(managerIds);
                team.setManagers(managers);
                teamRepository.save(team);
                businessChangeLogService.log(
                                "REASSIGN_TEAM_MANAGERS", "TEAM", teamId,
                                "managerIds", oldManagerIds, managerIds.toString());
        }
}