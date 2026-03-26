package com.das.skillmatrix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.annotation.LogActivity;
import com.das.skillmatrix.dto.request.TeamFilterRequest;
import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamDetailResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.repository.specification.TeamSpecification;

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
    private final BusinessChangeLogService businessChangeLogService;

    // ==================== PUBLIC METHODS ====================

    @LogActivity(action = "CREATE_TEAM", entityType = "TEAM")
    public TeamResponse create(TeamRequest req) {
        String name = normalizeName(req.getName());
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("TEAM_NAME_REQUIRED");
        }

        Department department = departmentRepository
                .findById(req.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("DEPARTMENT_NOT_FOUND"));

        if (department.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("DEPARTMENT_NOT_ACTIVE");
        }

        if (teamRepository.existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
                name,
                department.getDepartmentId(),
                List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE))) {
            throw new IllegalArgumentException("TEAM_NAME_EXISTS_IN_DEPARTMENT");
        }

        Team team = new Team();
        team.setName(name);
        team.setDescription(req.getDescription());
        team.setCreatedAt(LocalDateTime.now());
        team.setDepartment(department);
        team = teamRepository.save(team);

        return toResponse(team);
    }

    @LogActivity(action = "UPDATE_TEAM", entityType = "TEAM")
    public TeamResponse update(Long id, TeamRequest req) {
        Team team = getActiveTeamOrThrow(id);
        String newName = normalizeName(req.getName());
        if (newName == null || newName.isEmpty()) {
            throw new IllegalArgumentException("TEAM_NAME_REQUIRED");
        }

        Long oldDeptId = team.getDepartment().getDepartmentId();
        Long newDeptId = req.getDepartmentId();
        Department targetDepartment = team.getDepartment();

        if (!oldDeptId.equals(newDeptId)) {
            User currentUser = permissionService.getCurrentUser();

            if (permissionService.isTeamManagerOnly(currentUser)) {
                throw new IllegalArgumentException("MANAGER_TEAM_CANNOT_CHANGE_DEPARTMENT");
            }

            targetDepartment = departmentRepository
                    .findById(newDeptId)
                    .orElseThrow(() -> new IllegalArgumentException("DEPARTMENT_NOT_FOUND"));

            if (targetDepartment.getStatus() != GeneralStatus.ACTIVE) {
                throw new IllegalArgumentException("TARGET_DEPARTMENT_NOT_ACTIVE");
            }

            if (!permissionService.canMoveTeamDepartment(oldDeptId, newDeptId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "ACCESS_DENIED_TO_MOVE_DEPARTMENT");
            }

            team.setDepartment(targetDepartment);
        }

        if (!team.getName().equalsIgnoreCase(newName)) {
            if (teamRepository.existsByNameAndDepartmentIdExcluding(
                    newName,
                    targetDepartment.getDepartmentId(),
                    id,
                    List.of(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE))) {
                throw new IllegalArgumentException("TEAM_NAME_EXISTS_IN_DEPARTMENT");
            }
            team.setName(newName);
        }

        team.setDescription(req.getDescription());
        teamRepository.save(team);

        if (!oldDeptId.equals(newDeptId)) {
            businessChangeLogService.log(
                    "MIGRATE_TEAM_DEPARTMENT", "TEAM", id,
                    "departmentId", oldDeptId.toString(), newDeptId.toString());
        }

        return toResponse(team);
    }

    @LogActivity(action = "DELETE_TEAM", entityType = "TEAM")
    public void delete(Long id) {
        Team team = getActiveTeamOrThrow(id);
        String oldStatus = team.getStatus().name();

        boolean hasMembers = teamMemberRepository.existsByTeam_TeamId(id);

        if (hasMembers) {
            team.setStatus(GeneralStatus.DEACTIVE);
            team.setDeActiveAt(LocalDateTime.now());
        } else {
            team.setStatus(GeneralStatus.DELETED);
            team.setDeletedAt(LocalDateTime.now());
        }

        teamRepository.save(team);
        businessChangeLogService.log(
                "CHANGE_TEAM_STATUS", "TEAM", id,
                "status", oldStatus, team.getStatus().name());
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> list(TeamFilterRequest filter, Pageable pageable) {
        List<Long> departmentIds = null;

        if (filter.getDepartmentId() != null) {
            // Department-scoped search
            departmentRepository.findById(filter.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("DEPARTMENT_NOT_FOUND"));

            var spec = TeamSpecification.filterTeamsByDepartment(filter.getDepartmentId(), filter);
            var page = teamRepository.findAll(spec, pageable);

            List<TeamResponse> content = page.getContent().stream()
                    .map(this::toResponse)
                    .toList();
            return new PageResponse<>(content, page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
        }

        // Global search with scope resolution
        User currentUser = permissionService.getCurrentUser();

        if (!permissionService.isAdmin(currentUser)) {
            departmentIds = resolveDepartmentIds(currentUser);

            if (departmentIds.isEmpty()) {
                return new PageResponse<>(
                        List.of(), 0, pageable.getPageSize(),
                        0, 0, false, false);
            }
        }

        var spec = TeamSpecification.filterTeams(departmentIds, filter);
        var page = teamRepository.findAll(spec, pageable);

        List<TeamResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse detail(Long id) {
        Team team = getVisibleTeamOrThrow(id);
        long memberCount = teamMemberRepository.countByTeam_TeamId(id);
        return new TeamDetailResponse(
                team.getTeamId(),
                team.getName(),
                team.getDescription(),
                team.getDepartment().getDepartmentId(),
                team.getDepartment().getName(),
                team.getStatus(),
                team.getCreatedAt(),
                memberCount);
    }

    @LogActivity(action = "ADD_TEAM_MANAGER", entityType = "TEAM")
    public void addManager(Long teamId, Long userId) {
        Team team = getActiveTeamOrThrow(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (user.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("USER_NOT_ACTIVE");
        }
        if (!"MANAGER_TEAM".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("INVALID_MANAGER_ROLE");
        }
        boolean alreadyManager = team.getManagers().stream()
                .anyMatch(u -> u.getUserId().equals(userId));
        if (!alreadyManager) {
            team.getManagers().add(user);
            teamRepository.save(team);
            businessChangeLogService.log(
                    "ADD_TEAM_MANAGER", "TEAM", teamId,
                    "managerId", null, userId.toString());
        }
    }

    @LogActivity(action = "REMOVE_TEAM_MANAGER", entityType = "TEAM")
    public void removeManager(Long teamId, Long userId) {
        Team team = getActiveTeamOrThrow(teamId);
        boolean removed = team.getManagers().removeIf(u -> u.getUserId().equals(userId));
        if (removed) {
            teamRepository.save(team);
            businessChangeLogService.log(
                    "REMOVE_TEAM_MANAGER", "TEAM", teamId,
                    "managerId", userId.toString(), null);
        }
    }

    private Team getActiveTeamOrThrow(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TEAM_NOT_FOUND"));
        if (team.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
        }
        return team;
    }

    private Team getVisibleTeamOrThrow(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TEAM_NOT_FOUND"));
        if (team.getStatus() == GeneralStatus.DELETED) {
            throw new IllegalArgumentException("TEAM_NOT_FOUND");
        }
        return team;
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(
                team.getTeamId(),
                team.getName(),
                team.getDescription(),
                team.getDepartment().getDepartmentId(),
                team.getDepartment().getName(),
                team.getStatus(),
                team.getCreatedAt());
    }

    private List<Long> resolveDepartmentIds(User currentUser) {
        if (!currentUser.getManagedCareers().isEmpty()) {
            List<Long> careerIds = currentUser.getManagedCareers()
                    .stream()
                    .map(Career::getCareerId)
                    .toList();
            return departmentRepository.findByCareer_CareerIdIn(careerIds)
                    .stream()
                    .map(Department::getDepartmentId)
                    .toList();
        } else if (!currentUser.getManagedDepartments().isEmpty()) {
            List<Long> careerIds = currentUser.getManagedDepartments()
                    .stream()
                    .map(dept -> dept.getCareer().getCareerId())
                    .distinct()
                    .toList();
            return departmentRepository.findByCareer_CareerIdIn(careerIds)
                    .stream()
                    .map(Department::getDepartmentId)
                    .toList();
        } else if (!currentUser.getManagedTeams().isEmpty()) {
            return currentUser.getManagedTeams()
                    .stream()
                    .map(team -> team.getDepartment().getDepartmentId())
                    .distinct()
                    .toList();
        }
        return List.of();
    }
}