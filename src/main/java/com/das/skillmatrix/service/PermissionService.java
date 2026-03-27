package com.das.skillmatrix.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.User;
import java.util.List;

import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;
    private final CareerRepository careerRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    // ================= CAREER =================

    public boolean checkCareerAccess(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;
        return careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    // ================= DEPARTMENT =================

    public boolean checkDepartmentAccess(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        Long careerId = departmentRepository.findCareerIdByDepartmentId(departmentId).orElse(null);
        if (careerId == null) return false;

        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;

        return departmentRepository.existsByDepartmentIdAndManagers_UserId(departmentId, user.getUserId());
    }

    public boolean canManageDepartment(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        Long careerId = departmentRepository.findCareerIdByDepartmentId(departmentId).orElse(null);
        if (careerId == null) return false;

        return careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    public boolean canViewDepartmentList(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;

        return departmentRepository.existsByCareer_CareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    public boolean canViewDepartmentDetail(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        Long careerId = departmentRepository.findCareerIdByDepartmentId(departmentId).orElse(null);
        if (careerId == null) return false;

        return canViewDepartmentList(careerId);
    }

    public boolean canMoveDepartment(Long sourceCareerId, Long targetCareerId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;
        return checkCareerAccess(sourceCareerId) && checkCareerAccess(targetCareerId);
    }

    public boolean canManageDepartment_byCareerId(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;
        return careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    public boolean isManagerDepartmentOnly() {
        User user = getCurrentUser();
        if (isAdmin(user)) return false;
        if (!user.getManagedCareers().isEmpty()) return false;
        return !user.getManagedDepartments().isEmpty();
    }

    // ================= TEAM =================

    public boolean checkTeamAccess(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        Long careerId = teamRepository.findCareerIdByTeamId(teamId).orElse(null);
        if (careerId == null) return false;

        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;

        Long departmentId = teamRepository.findDepartmentIdByTeamId(teamId).orElse(null);

        if (departmentId != null &&
            departmentRepository.existsByDepartmentIdAndManagers_UserId(departmentId, user.getUserId()))
            return true;

        return teamRepository.existsByTeamIdAndManagers_UserId(teamId, user.getUserId());
    }

    public boolean checkTeamMemberAccess(Long teamMemberId) {
        Long teamId = teamMemberRepository.findById(teamMemberId).map(tm -> tm.getTeam().getTeamId()).orElse(null);
        if (teamId == null) return true;
        return checkTeamAccess(teamId);
    }

    public boolean checkMultiTeamAccess(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) return true;
        for (Long teamId : teamIds) {
            if (!checkTeamAccess(teamId)) return false;
        }
        return true;
    }

    public boolean canManageTeam(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        Long careerId = teamRepository.findCareerIdByTeamId(teamId).orElse(null);
        if (careerId == null) return false;

        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;

        Long departmentId = teamRepository.findDepartmentIdByTeamId(teamId).orElse(null);

        return departmentId != null &&
               departmentRepository.existsByDepartmentIdAndManagers_UserId(departmentId, user.getUserId());
    }

    // ================= MASTER EXTRA METHODS =================

    public String getManagerType(User user) {
        if (isAdmin(user)) return "ADMIN";
        if (!user.getManagedCareers().isEmpty()) return "MANAGER_CAREER";
        if (!user.getManagedDepartments().isEmpty()) return "MANAGER_DEPARTMENT";
        if (!user.getManagedTeams().isEmpty()) return "MANAGER_TEAM";
        return "NONE";
    }

    public boolean canMoveTeamDepartment(Long currentDepartmentId, Long targetDepartmentId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        if (!user.getManagedCareers().isEmpty()) {
            return checkDepartmentAccess(currentDepartmentId)
                && checkDepartmentAccess(targetDepartmentId);
        }

        if (!user.getManagedDepartments().isEmpty()) {
            return departmentRepository.existsByDepartmentIdAndManagers_UserId(currentDepartmentId, user.getUserId())
                && departmentRepository.existsByDepartmentIdAndManagers_UserId(targetDepartmentId, user.getUserId());
        }

        return false;
    }

    public boolean isTeamManagerOnly(User user) {
        return !isAdmin(user)
            && user.getManagedCareers().isEmpty()
            && user.getManagedDepartments().isEmpty()
            && !user.getManagedTeams().isEmpty();
    }

    public boolean checkTeamViewAccess(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user)) return true;

        Long careerId = teamRepository.findCareerIdByTeamId(teamId).orElse(null);
        if (careerId == null) return false;

        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;

        Long departmentId = teamRepository.findDepartmentIdByTeamId(teamId).orElse(null);

        if (departmentId == null) return false;

        for (Department dept : user.getManagedDepartments()) {
            if (dept.getDepartmentId().equals(departmentId)) return true;
        }

        for (Team team : user.getManagedTeams()) {
            if (team.getTeamId().equals(teamId)) return true;
        }

        return false;
    }

    // ================= COMMON =================

    public User getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findUserByEmail(auth.getName());
    }

    public boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }

    // ================== USER PERMISSIONS ==================
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean checkUserViewAccess(Long targetUserId) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;
        
        if (currentUser.getUserId().equals(targetUserId)) return true;
        
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return false;
        
        switch (currentUser.getRole()) {
            case "MANAGER_CAREER":
                return validateScopeInManagersList(targetUser.getDepartment(), null, currentUser.getManagedCareers(), null);
                
            case "MANAGER_DEPARTMENT":
                return validateScopeInManagersList(targetUser.getDepartment(), null, null, currentUser.getManagedDepartments());
                
            case "MANAGER_TEAM":
                if (targetUser.getDepartment() == null) return false;
                return currentUser.getManagedTeams().stream().anyMatch(cmt -> 
                    cmt.getDepartment().getDepartmentId().equals(targetUser.getDepartment().getDepartmentId()));
                                
            case "STAFF":
                // Staff can only view users in teams they belong to
                List<com.das.skillmatrix.entity.TeamMember> staffTeams = teamMemberRepository.findByUser_UserId(currentUser.getUserId());
                List<com.das.skillmatrix.entity.TeamMember> targetTeams = teamMemberRepository.findByUser_UserId(targetUserId);
                return staffTeams.stream().anyMatch(st -> 
                    targetTeams.stream().anyMatch(tt -> st.getTeam().getTeamId().equals(tt.getTeam().getTeamId())));
            default:
                return false;
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean canManageUser(Long targetUserId) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;
        
        // Manager Team and Staff cannot edit/delete any user (4.4.1, 4.5.1)
        String role = currentUser.getRole();
        if ("MANAGER_TEAM".equals(role) || "STAFF".equals(role)) return false;
        
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return false;
        
        if ("MANAGER_CAREER".equals(currentUser.getRole())) {
            return validateScopeInManagersList(targetUser.getDepartment(), null, currentUser.getManagedCareers(), null);
        }
        
        if ("MANAGER_DEPARTMENT".equals(currentUser.getRole())) {
            return validateScopeInManagersList(targetUser.getDepartment(), null, null, currentUser.getManagedDepartments());
        }
        
        return false;
    }
    
    private boolean validateScopeInManagersList(Department targetDept, Team targetTeam, java.util.List<com.das.skillmatrix.entity.Career> managedCareers, java.util.List<Department> managedDepts) {
        if (targetDept == null && targetTeam == null) return false;
        
        if (targetTeam != null) {
            if (managedDepts != null) {
                return managedDepts.stream().anyMatch(d -> d.getDepartmentId().equals(targetTeam.getDepartment().getDepartmentId()));
            }
            if (managedCareers != null) {
                return managedCareers.stream().anyMatch(c -> c.getCareerId().equals(targetTeam.getDepartment().getCareer().getCareerId()));
            }
        } 
        else if (targetDept != null) {
            if (managedCareers != null) {
                return managedCareers.stream().anyMatch(c -> c.getCareerId().equals(targetDept.getCareer().getCareerId()));
            }
            if (managedDepts != null) {
                return managedDepts.stream().anyMatch(old -> old.getDepartmentId().equals(targetDept.getDepartmentId()));
            }
        }
        return false;
    }
}