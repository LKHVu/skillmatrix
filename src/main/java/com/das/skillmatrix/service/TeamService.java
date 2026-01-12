package com.das.skillmatrix.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        public TeamService(TeamRepository teamRepository, UserRepository userRepository,
                        TeamMemberRepository teamMemberRepository, DepartmentRepository departmentRepository) {
                this.teamRepository = teamRepository;
                this.teamMemberRepository = teamMemberRepository;
                this.userRepository = userRepository;
                this.departmentRepository = departmentRepository;
        }

        // Convert Team to TeamResponse
        private TeamResponse toTeamResponse(Team team, User manager, Department department) {
                return new TeamResponse(
                                team.getTeamId(),
                                team.getName(),
                                team.getDescription(),
                                new TeamResponse.Manager(manager.getUserId(), manager.getEmail(),
                                                manager.getFullName()),
                                new TeamResponse.Department(department.getDepartmentId(), department.getName(),
                                                department.getDescription()));
        }

        public TeamResponse createTeam(TeamRequest teamRequest) {
                User manager = userRepository.findById(teamRequest.getManagerId())
                                .orElseThrow(() -> new ResourceNotFoundException("MANAGER_NOT_FOUND"));
                Department department = departmentRepository.findById(teamRequest.getDepartmentId())
                                .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
                Team team = new Team();
                team.setName(teamRequest.getName());
                team.setDescription(teamRequest.getDescription());
                team.setManager(manager);
                team.setDepartment(department);
                teamRepository.save(team);

                return toTeamResponse(team, manager, department);
        }

        public TeamResponse updateTeam(Long teamId, TeamRequest teamRequest) {
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
                User manager = userRepository.findById(teamRequest.getManagerId())
                                .orElseThrow(() -> new ResourceNotFoundException("MANAGER_NOT_FOUND"));
                Department department = departmentRepository.findById(teamRequest.getDepartmentId())
                                .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
                team.setName(teamRequest.getName());
                team.setDescription(teamRequest.getDescription());
                team.setManager(manager);
                team.setDepartment(department);
                teamRepository.save(team);

                return toTeamResponse(team, manager, department);
        }

        public PageResponse<TeamResponse> getAllTeams(Pageable pageable) {
                Page<Team> teams = teamRepository.findAll(pageable);
                // Convert Team to TeamResponse
                List<TeamResponse> teamResponses = teams.stream()
                                .map(team -> toTeamResponse(team, team.getManager(), team.getDepartment()))
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
                return toTeamResponse(team, team.getManager(), team.getDepartment());
        }

        public void deleteTeam(Long teamId) {
                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
                teamMemberRepository.deleteAllByTeam(team);
                teamRepository.delete(team);
        }

}