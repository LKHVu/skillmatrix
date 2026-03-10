package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.request.criteria.TeamSearchCriteria;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.service.helper.TeamSearchHelper;
import com.querydsl.core.BooleanBuilder;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionService permissionService;

    // Convert Team to TeamResponse
    private static TeamResponse toTeamResponse(Team team, Department department) {
        return new TeamResponse(
                team.getTeamId(),
                team.getName(),
                team.getDescription(),
                team.getStatus(),
                team.getCreatedAt(),
                new TeamResponse.Department(department.getDepartmentId(), department.getName()));
    }

    // Resolve department IDs from user's managed careers, departments, and teams
    private List<Long> resolveDepartmentIds(User currentUser) {
        if (!currentUser.getManagedCareers().isEmpty()) {
            List<Long> careerIds = currentUser.getManagedCareers().stream()
                    .map(Career::getCareerId).toList();
            return departmentRepository.findByCareer_CareerIdIn(careerIds).stream()
                    .map(Department::getDepartmentId).toList();
        } else if (!currentUser.getManagedDepartments().isEmpty()) {
            List<Long> careerIds = currentUser.getManagedDepartments().stream()
                    .map(dept -> dept.getCareer().getCareerId())
                    .distinct().toList();
            return departmentRepository.findByCareer_CareerIdIn(careerIds).stream()
                    .map(Department::getDepartmentId).toList();
        } else if (!currentUser.getManagedTeams().isEmpty()) {
            return currentUser.getManagedTeams().stream()
                    .map(team -> team.getDepartment().getDepartmentId())
                    .distinct().toList();
        }
        return List.of();
    }

    public TeamResponse createTeam(TeamRequest teamRequest) {
        String name = teamRequest.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("TEAM_NAME_REQUIRED");
        }
        Department department = departmentRepository.findById(teamRequest.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
        if (department.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("DEPARTMENT_NOT_ACTIVE");
        }
        List<GeneralStatus> activeStatuses = List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE);
        if (teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(name, department.getDepartmentId(), activeStatuses)) {
            throw new IllegalArgumentException("TEAM_NAME_EXISTS_IN_DEPARTMENT");
        }
        Team team = new Team();
        team.setName(teamRequest.getName());
        team.setDescription(teamRequest.getDescription());
        team.setCreatedAt(LocalDateTime.now());
        team.setDepartment(department);
        teamRepository.save(team);
        return toTeamResponse(team, department);
    }

    public TeamResponse updateTeam(Long teamId, TeamRequest teamRequest) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
        String newName = teamRequest.getName().trim();
        if (newName.isEmpty()) {
            throw new IllegalArgumentException("TEAM_NAME_REQUIRED");
        }
        if (team.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
        }
        // Handle department change
        Department targetDepartment = team.getDepartment();
        boolean departmentChanged = !team.getDepartment().getDepartmentId().equals(teamRequest.getDepartmentId());
        if (departmentChanged) {
            User currentUser = permissionService.getCurrentUser();
            // Manager Team cannot change department
            if (permissionService.isTeamManagerOnly(currentUser)) {
                throw new IllegalArgumentException("MANAGER_TEAM_CANNOT_CHANGE_DEPARTMENT");
            }
            // Validate new department
            targetDepartment = departmentRepository.findById(teamRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
            if (targetDepartment.getStatus() != GeneralStatus.ACTIVE) {
                throw new IllegalArgumentException("TARGET_DEPARTMENT_NOT_ACTIVE");
            }
            // Validate permission to move department
            if (!permissionService.canMoveTeamDepartment(
                    team.getDepartment().getDepartmentId(), targetDepartment.getDepartmentId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "ACCESS_DENIED_TO_MOVE_DEPARTMENT");
            }
        }
        // Check unique name in target department
        List<GeneralStatus> activeStatuses = List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE);
        if (teamRepository.existsByNameAndDepartmentIdExcluding(
                newName, targetDepartment.getDepartmentId(), teamId, activeStatuses)) {
            throw new IllegalArgumentException("TEAM_NAME_EXISTS_IN_DEPARTMENT");
        }
        
        team.setName(newName);
        team.setDescription(teamRequest.getDescription());
        if (departmentChanged) {
           team.setDepartment(targetDepartment); 
        }
        teamRepository.save(team);
        return toTeamResponse(team, targetDepartment);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> getAllTeams(TeamSearchCriteria criteria,Pageable pageable) {
        User currentUser = permissionService.getCurrentUser();
        List<Long> departmentIds = null;
        if (!permissionService.isAdmin(currentUser)) {
            departmentIds = resolveDepartmentIds(currentUser);
            // If user has no managed departments, return empty page
            if (departmentIds.isEmpty()) {
                return new PageResponse<>(List.of(), 0, pageable.getPageSize(), 0, 0, false, false);
            }
        }
        BooleanBuilder predicate = TeamSearchHelper.buildGlobalSearch(departmentIds, criteria);
        Page<Team> teams = teamRepository.findAll(predicate, pageable);
        List<TeamResponse> responses = teams.stream()
            .map(team -> toTeamResponse(team, team.getDepartment()))
            .toList();
        return new PageResponse<>(
            responses, teams.getNumber(), teams.getSize(),
            teams.getTotalElements(), teams.getTotalPages(),
            teams.hasNext(), teams.hasPrevious());
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> getTeamsByDepartment(TeamSearchCriteria criteria, Pageable pageable) { 
        Department department = departmentRepository.findById(criteria.getDepartmentId())
            .orElseThrow(() -> new ResourceNotFoundException("DEPARTMENT_NOT_FOUND"));
        if (department.getStatus() == GeneralStatus.DELETED) {
            throw new ResourceNotFoundException("DEPARTMENT_NOT_FOUND");
        }
        BooleanBuilder predicate = TeamSearchHelper.buildDepartmentSearch(criteria);
        Page<Team> teams = teamRepository.findAll(predicate, pageable);
        List<TeamResponse> responses = teams.stream()
            .map(team -> toTeamResponse(team, team.getDepartment()))
            .toList();
        return new PageResponse<>(
            responses, teams.getNumber(), teams.getSize(),
            teams.getTotalElements(), teams.getTotalPages(),
            teams.hasNext(), teams.hasPrevious());
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
        if (team.getStatus() == GeneralStatus.DELETED) {
            throw new ResourceNotFoundException("TEAM_NOT_FOUND");
        }
        long memberCount = teamMemberRepository.countByTeam_TeamId(teamId);

        return new TeamDetailResponse(
            team.getTeamId(),
            team.getName(),
            team.getDescription(),
            team.getStatus(),
            team.getCreatedAt(),
            memberCount,
            new TeamDetailResponse.Department(
                team.getDepartment().getDepartmentId(),
                team.getDepartment().getName()));
    }

    public String deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND"));
        // Only delete active team
        if (team.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
        }
        // Check team has members
        boolean hasMembers = teamMemberRepository.existsByTeam_TeamId(teamId);
        if (hasMembers) {
            // Team has members -> DEACTIVE
            team.setStatus(GeneralStatus.DEACTIVE);
            team.setDeActiveAt(LocalDateTime.now());
            teamRepository.save(team);
            return "Team has been deactivated successfully.";
        } else {
            // Team has no members -> DELETED (soft-delete 30 days)
            team.setStatus(GeneralStatus.DELETED);
            team.setDeletedAt(LocalDateTime.now());
            teamRepository.save(team);
            return "Team deleted successfully.";
        }
    }

    public void assignManagers(Long teamId, List<Long> managerIds) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("TEAM_NOT_FOUND"));

        // Check permissions: Must be Admin OR Career Manager OR Department Manager
        // Note: The controller typically handles the permission check (e.g.
        // PermissionService.checkDepartmentAccess)
        // We assume Controller handles the @PreAuthorize check.
        List<User> managers = userRepository.findAllById(managerIds);
        team.setManagers(managers);
        teamRepository.save(team);
    }
}