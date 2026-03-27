package com.das.skillmatrix.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.das.skillmatrix.annotation.LogActivity;
import com.das.skillmatrix.dto.request.CreateUserRequest;
import com.das.skillmatrix.dto.request.DeactivateUserRequest;
import com.das.skillmatrix.dto.request.UpdateUserRequest;
import com.das.skillmatrix.dto.request.UserFilterRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.PositionBrief;
import com.das.skillmatrix.dto.response.UserDetailResponse;
import com.das.skillmatrix.dto.response.UserResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Position;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.TeamMember;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.exception.ResourceNotFoundException;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.PositionRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;
import com.das.skillmatrix.repository.specification.UserSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final CareerRepository careerRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final BusinessChangeLogService logService;
    
    private static final String PWD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(UserFilterRequest filter, Pageable pageable) {
        User currentUser = permissionService.getCurrentUser();
        List<Long> scopeIds = resolveUserScopeIds(currentUser);
        Specification<User> spec = UserSpecification.filterUsers(filter, scopeIds, currentUser.getRole());
        Page<User> page = userRepository.findAll(spec, pageable);
        List<UserResponse> data = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(
                data,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listByTeam(Long teamId, UserFilterRequest filter, Pageable pageable) {
        if (!teamRepository.existsById(teamId)) {
            throw new IllegalArgumentException("TEAM_NOT_FOUND");
        }
        filter.setTeamId(teamId);
        return list(filter, pageable);
    }

    @Transactional
    @LogActivity(action = "CREATE_USER", entityType = "USER")
    public UserResponse create(CreateUserRequest req) {
        User currentUser = permissionService.getCurrentUser();
        validateEmailUnique(req.getEmail(), null);
        validateCreatePermission(currentUser, req);
        List<Position> positions = fetchAndValidatePositions(req.getPositionIds(), req.getRole());
        User user = new User();
        user.setEmail(req.getEmail().trim().toLowerCase());
        String rawPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        log.debug("Generated password for new user: {}", rawPassword);
        user.setRole(req.getRole());
        user.setStatus(GeneralStatus.ACTIVE);
        if (!positions.isEmpty()) {
            user.setPositions(new ArrayList<>(positions));
        }
        user = userRepository.save(user);
        assignScope(user, req.getCareerId(), req.getDepartmentId(), req.getTeamId());
        List<BusinessChangeLogService.FieldChange> fieldChanges = List.of(
            new BusinessChangeLogService.FieldChange("email", null, user.getEmail()),
            new BusinessChangeLogService.FieldChange("role", null, user.getRole())
        );
        logService.log("CREATE_USER", "USER", user.getUserId(), fieldChanges);
        return toResponse(user);
    }

    @Transactional
    @LogActivity(action = "UPDATE_USER", entityType = "USER")
    public UserResponse update(Long userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        User currentUser = permissionService.getCurrentUser();
        validateUpdatePermission(currentUser, user, req);
        validateEmailUnique(req.getEmail(), userId);
        List<Position> positions = fetchAndValidatePositions(req.getPositionIds(), req.getRole());
        List<BusinessChangeLogService.FieldChange> fieldChanges = new ArrayList<>();
        String oldEmail = user.getEmail();
        String oldRole = user.getRole();
        if (!oldEmail.equalsIgnoreCase(req.getEmail())) {
            fieldChanges.add(new BusinessChangeLogService.FieldChange("email", oldEmail, req.getEmail()));
        }
        boolean roleChanged = !oldRole.equals(req.getRole());
        if (roleChanged) {
            fieldChanges.add(new BusinessChangeLogService.FieldChange("role", oldRole, req.getRole()));
        }
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setRole(req.getRole());
        if (roleChanged || scopeChanged(user, req)) {
            assignScope(user, req.getCareerId(), req.getDepartmentId(), req.getTeamId());
            fieldChanges.add(new BusinessChangeLogService.FieldChange("scope", "old", "updated"));
        }
        if (!positions.isEmpty()) {
            user.setPositions(new ArrayList<>(positions));
            fieldChanges.add(new BusinessChangeLogService.FieldChange("positions", "old", "updated"));
        } else if ("ADMIN".equals(req.getRole())) {
            user.getPositions().clear();
        }
        user = userRepository.save(user);
        if (!fieldChanges.isEmpty()) {
            logService.log("UPDATE_USER", "USER", user.getUserId(), fieldChanges);
        }
        return toResponse(user);
    }

    @Transactional
    @LogActivity(action = "DELETE_USER", entityType = "USER")
    public void deactivateOrDelete(Long userId, DeactivateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (user.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("CANNOT_DEACTIVE_DEACTIVED_USER");
        }
        if ("DELETE".equalsIgnoreCase(req.getAction())) {
            userRepository.delete(user);
            logService.log("DELETE_USER", "USER", userId, "status", GeneralStatus.ACTIVE.name(), "DELETED");
            return;
        }
        if ("DEACTIVE".equalsIgnoreCase(req.getAction())) {
            user.setStatus(GeneralStatus.DEACTIVE);
            user.setDeActiveAt(LocalDateTime.now());
            if ("TEMPORARY".equalsIgnoreCase(req.getDeactiveType())) {
                if (!StringUtils.hasText(req.getDuration())) {
                    throw new IllegalArgumentException("DEACTIVATION_DURATION_REQUIRED");
                }
                user.setDeactiveType("TEMPORARY");
                user.setDeactiveUntil(calculateDeactivationEndDate(req.getDuration()));
            } else {
                user.setDeactiveType("UNLIMITED");
                user.setDeactiveUntil(null);
            }
            userRepository.save(user);
            logService.log("DEACTIVATE_USER", "USER", userId, "status", 
                    GeneralStatus.ACTIVE.name(), GeneralStatus.DEACTIVE.name());
            return;
        }
        throw new IllegalArgumentException("INVALID_ACTION");
    }

    @Transactional
    @LogActivity(action = "REACTIVATE_USER", entityType = "USER")
    public UserResponse reactivate(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (user.getStatus() == GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("USER_NOT_DEACTIVE");
        }
        user.setStatus(GeneralStatus.ACTIVE);
        user.setDeactiveType(null);
        user.setDeactiveUntil(null);
        user.setDeActiveAt(null);
        userRepository.save(user);
        logService.log("REACTIVATE_USER", "USER", userId, "status", GeneralStatus.DEACTIVE.name(), GeneralStatus.ACTIVE.name());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getDetail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));
        return toDetailResponse(user);
    }

    private void validateEmailUnique(String email, Long excludeUserId) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("EMAIL_REQUIRED");
        }
        boolean exists = excludeUserId == null 
                ? userRepository.existsByEmailIgnoreCase(email) 
                : userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, excludeUserId);
        if (exists) {
            throw new IllegalArgumentException("EMAIL_ALREADY_EXISTS");
        }
    }

    private String generateRandomPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        StringBuilder sb = new StringBuilder(10);
        sb.append(upper.charAt(secureRandom.nextInt(upper.length())));
        sb.append(lower.charAt(secureRandom.nextInt(lower.length())));
        sb.append(digits.charAt(secureRandom.nextInt(digits.length())));
        for (int i = 3; i < 10; i++) {
            sb.append(PWD_CHARS.charAt(secureRandom.nextInt(PWD_CHARS.length())));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    private List<Position> fetchAndValidatePositions(List<Long> positionIds, String role) {
        if ("ADMIN".equals(role)) {
            return new ArrayList<>();
        }
        if (positionIds == null || positionIds.isEmpty()) {
            throw new IllegalArgumentException("POSITION_REQUIRED");
        }
        List<Position> positions = positionRepository.findByPositionIdInAndStatus(positionIds, GeneralStatus.ACTIVE);
        if (positions.size() != positionIds.size()) {
            throw new IllegalArgumentException("POSITION_NOT_ACTIVE");
        }
        return positions;
    }

    private void assignScope(User user, Long careerId, Long departmentId, Long teamId) {
        String role = user.getRole();
        user.getManagedCareers().forEach(c -> c.getManagers().remove(user));
        user.getManagedDepartments().forEach(d -> d.getManagers().remove(user));
        user.getManagedTeams().forEach(t -> t.getManagers().remove(user));
        user.getManagedCareers().clear();
        user.getManagedDepartments().clear();
        user.getManagedTeams().clear();
        user.setDepartment(null);
        switch (role) {
            case "ADMIN":
                break;
            case "MANAGER_CAREER":
                if (careerId == null) throw new IllegalArgumentException("SCOPE_REQUIRED");
                Career career = careerRepository.findById(careerId).orElseThrow(() -> new IllegalArgumentException("INVALID_SCOPE"));
                if (career.getStatus() != GeneralStatus.ACTIVE) throw new IllegalArgumentException("INVALID_SCOPE");
                career.getManagers().add(user);
                user.getManagedCareers().add(career);
                break;
            case "MANAGER_DEPARTMENT":
                if (departmentId == null) throw new IllegalArgumentException("SCOPE_REQUIRED");
                Department dept = departmentRepository.findById(departmentId).orElseThrow(() -> new IllegalArgumentException("INVALID_SCOPE"));
                if (dept.getStatus() != GeneralStatus.ACTIVE) throw new IllegalArgumentException("INVALID_SCOPE");
                dept.getManagers().add(user);
                user.getManagedDepartments().add(dept);
                user.setDepartment(dept);
                break;
            case "MANAGER_TEAM":
                if (teamId == null) throw new IllegalArgumentException("SCOPE_REQUIRED");
                Team tmTeam = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("INVALID_SCOPE"));
                if (tmTeam.getStatus() != GeneralStatus.ACTIVE) throw new IllegalArgumentException("INVALID_SCOPE");
                tmTeam.getManagers().add(user);
                user.getManagedTeams().add(tmTeam);
                user.setDepartment(tmTeam.getDepartment());
                break;
            case "STAFF":
                if (teamId == null) throw new IllegalArgumentException("SCOPE_REQUIRED");
                Team sTeam = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("INVALID_SCOPE"));
                if (sTeam.getStatus() != GeneralStatus.ACTIVE) throw new IllegalArgumentException("INVALID_SCOPE");
                user.setDepartment(sTeam.getDepartment());
                if (!teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(teamId, user.getUserId())) {
                    TeamMember newMember = new TeamMember();
                    newMember.setTeam(sTeam);
                    newMember.setUser(user);
                    newMember.setPosition(user.getPositions().isEmpty() ? null : user.getPositions().get(0));
                    teamMemberRepository.save(newMember);
                }
                break;
            default:
                throw new IllegalArgumentException("INVALID_ROLE");
        }
    }

    private void validateCreatePermission(User creator, CreateUserRequest req) {
        if ("ADMIN".equals(creator.getRole())) {
            return;
        }
        if ("MANAGER_CAREER".equals(creator.getRole())) {
            if ("ADMIN".equals(req.getRole()) || "MANAGER_CAREER".equals(req.getRole())) {
                throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
            }
            validateScopeInManagersList(req.getDepartmentId(), req.getTeamId(), creator.getManagedCareers(), null);
            return;
        }
        if ("MANAGER_DEPARTMENT".equals(creator.getRole())) {
            if ("ADMIN".equals(req.getRole()) || "MANAGER_CAREER".equals(req.getRole()) || "MANAGER_DEPARTMENT".equals(req.getRole())) {
                throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
            }
            validateScopeInManagersList(null, req.getTeamId(), null, creator.getManagedDepartments());
            return;
        }
        throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
    }

    private void validateUpdatePermission(User editor, User target, UpdateUserRequest req) {
        if (editor.getUserId().equals(target.getUserId())) {
            if (!editor.getRole().equals(req.getRole())) {
                throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
            }
            return;
        }
        if ("ADMIN".equals(editor.getRole())) return;
        validateCreatePermission(editor, convertToCreateReq(req));
    }
    
    private CreateUserRequest convertToCreateReq(UpdateUserRequest req) {
        CreateUserRequest c = new CreateUserRequest();
        c.setRole(req.getRole());
        c.setCareerId(req.getCareerId());
        c.setDepartmentId(req.getDepartmentId());
        c.setTeamId(req.getTeamId());
        return c;
    }

    private void validateScopeInManagersList(Long targetDeptId, Long targetTeamId, List<Career> managedCareers, List<Department> managedDepts) {
        if (targetTeamId != null) {
            Team t = teamRepository.findById(targetTeamId).orElseThrow(() -> new IllegalArgumentException("INVALID_SCOPE"));
            if (managedDepts != null) {
                boolean hasAccess = managedDepts.stream().anyMatch(d -> d.getDepartmentId().equals(t.getDepartment().getDepartmentId()));
                if (!hasAccess) throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
            }
            if (managedCareers != null) {
                boolean hasAccess = managedCareers.stream().anyMatch(c -> c.getCareerId().equals(t.getDepartment().getCareer().getCareerId()));
                if (!hasAccess) throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
            }
        } 
        else if (targetDeptId != null) {
            Department d = departmentRepository.findById(targetDeptId).orElseThrow(() -> new IllegalArgumentException("INVALID_SCOPE"));
            if (managedCareers != null) {
                boolean hasAccess = managedCareers.stream().anyMatch(c -> c.getCareerId().equals(d.getCareer().getCareerId()));
                if (!hasAccess) throw new IllegalArgumentException("ROLE_EXCEEDS_PERMISSION");
            }
        }
    }

    private boolean scopeChanged(User user, UpdateUserRequest req) {
        String role = user.getRole();
        switch (role) {
            case "ADMIN":
                return false;
            case "MANAGER_CAREER":
                if (user.getManagedCareers().isEmpty()) return req.getCareerId() != null;
                return !user.getManagedCareers().get(0).getCareerId().equals(req.getCareerId());
            case "MANAGER_DEPARTMENT":
                if (user.getManagedDepartments().isEmpty()) return req.getDepartmentId() != null;
                return !user.getManagedDepartments().get(0).getDepartmentId().equals(req.getDepartmentId());
            case "MANAGER_TEAM":
                if (user.getManagedTeams().isEmpty()) return req.getTeamId() != null;
                return !user.getManagedTeams().get(0).getTeamId().equals(req.getTeamId());
            case "STAFF":
                List<TeamMember> members = teamMemberRepository.findByUser_UserId(user.getUserId());
                if (members.isEmpty()) return req.getTeamId() != null;
                return !members.get(0).getTeam().getTeamId().equals(req.getTeamId());
            default:
                return true;
        }
    }

    private LocalDateTime calculateDeactivationEndDate(String duration) {
        LocalDateTime now = LocalDateTime.now();
        switch (duration.toUpperCase()) {
            case "3_DAYS": return now.plusDays(3);
            case "7_DAYS": return now.plusDays(7);
            case "1_MONTH": return now.plusMonths(1);
            case "3_MONTHS": return now.plusMonths(3);
            case "6_MONTHS": return now.plusMonths(6);
            case "1_YEAR": return now.plusYears(1);
            default: throw new IllegalArgumentException("DEACTIVATION_DURATION_REQUIRED");
        }
    }

    private List<Long> resolveUserScopeIds(User user) {
        switch (user.getRole()) {
            case "MANAGER_CAREER": return user.getManagedCareers().stream().map(Career::getCareerId).toList();
            case "MANAGER_DEPARTMENT": return user.getManagedDepartments().stream().map(Department::getDepartmentId).toList();
            case "MANAGER_TEAM": return user.getManagedTeams().stream().map(Team::getTeamId).toList();
            case "STAFF": 
                return teamMemberRepository.findByUser_UserId(user.getUserId())
                    .stream().map(tm -> tm.getTeam().getTeamId()).toList();
            default: return new ArrayList<>();
        }
    }

    private UserResponse toResponse(User user) {
        UserResponse res = new UserResponse();
        res.setUserId(user.getUserId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setUserAvatar(user.getUserAvatar());
        res.setRole(user.getRole());
        res.setStatus(user.getStatus());
        res.setCreatedAt(user.getCreatedAt());
        List<PositionBrief> pbList = user.getPositions().stream()
            .map(p -> new PositionBrief(p.getPositionId(), p.getName()))
            .collect(Collectors.toList());
        res.setPositions(pbList);
        return res;
    }

    private UserDetailResponse toDetailResponse(User user) {
        UserDetailResponse res = new UserDetailResponse();
        res.setUserId(user.getUserId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setUserAvatar(user.getUserAvatar());
        res.setPhone(user.getPhone());
        res.setRole(user.getRole());
        res.setStatus(user.getStatus());
        res.setDeactiveType(user.getDeactiveType());
        res.setDeactiveUntil(user.getDeactiveUntil());
        res.setCreatedAt(user.getCreatedAt());
        List<PositionBrief> pbList = user.getPositions().stream()
            .map(p -> new PositionBrief(p.getPositionId(), p.getName()))
            .collect(Collectors.toList());
        res.setPositions(pbList);
        if (!user.getManagedCareers().isEmpty()) {
            res.setCareerId(user.getManagedCareers().get(0).getCareerId());
            res.setCareerName(user.getManagedCareers().get(0).getName());
        } else if (!user.getManagedDepartments().isEmpty()) {
            res.setDepartmentId(user.getManagedDepartments().get(0).getDepartmentId());
            res.setDepartmentName(user.getManagedDepartments().get(0).getName());
        } else if (!user.getManagedTeams().isEmpty()) {
            res.setTeamId(user.getManagedTeams().get(0).getTeamId());
            res.setTeamName(user.getManagedTeams().get(0).getName());
        } else if (user.getDepartment() != null) {
            List<TeamMember> tms = teamMemberRepository.findByUser_UserId(user.getUserId());
            if (!tms.isEmpty()) {
                res.setTeamId(tms.get(0).getTeam().getTeamId());
                res.setTeamName(tms.get(0).getTeam().getName());
            }
        }
        return res;
    }
}