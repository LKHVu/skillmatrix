package com.das.skillmatrix.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.CareerRepository;
import com.das.skillmatrix.repository.DepartmentRepository;
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

    // CAREER
    public boolean checkCareerAccess(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        return careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    // DEPARTMENT
    public boolean checkDepartmentAccess(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Long careerId = departmentRepository.findCareerIdByDepartmentId(departmentId).orElse(null);
        if (careerId == null)
            return false;
        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;
        return departmentRepository.existsByDepartmentIdAndManagers_UserId(departmentId, user.getUserId());
    }

    public boolean canManageDepartment(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Long careerId = departmentRepository.findCareerIdByDepartmentId(departmentId).orElse(null);
        if (careerId == null)
            return false;
        return careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    public boolean canViewDepartmentList(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;
        return departmentRepository.existsByCareer_CareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    public boolean canViewDepartmentDetail(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Long careerId = departmentRepository.findCareerIdByDepartmentId(departmentId).orElse(null);
        if (careerId == null)
            return false;
        return canViewDepartmentList(careerId);
    }

    // TEAM
    public boolean checkTeamAccess(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Long careerId = teamRepository.findCareerIdByTeamId(teamId).orElse(null);
        if (careerId == null)
            return false;
        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;
        Long departmentId = teamRepository.findDepartmentIdByTeamId(teamId).orElse(null);
        if (departmentId != null
                && departmentRepository.existsByDepartmentIdAndManagers_UserId(departmentId, user.getUserId()))
            return true;
        return teamRepository.existsByTeamIdAndManagers_UserId(teamId, user.getUserId());
    }

    public boolean canManageTeam(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Long careerId = teamRepository.findCareerIdByTeamId(teamId).orElse(null);
        if (careerId == null)
            return false;
        if (careerRepository.existsByCareerIdAndManagers_UserId(careerId, user.getUserId()))
            return true;
        Long departmentId = teamRepository.findDepartmentIdByTeamId(teamId).orElse(null);
        return departmentId != null
                && departmentRepository.existsByDepartmentIdAndManagers_UserId(departmentId, user.getUserId());
    }

    public boolean canMoveDepartment(Long sourceCareerId, Long targetCareerId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        return checkCareerAccess(sourceCareerId) && checkCareerAccess(targetCareerId);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        return userRepository.findUserByEmail(auth.getName());
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }
}