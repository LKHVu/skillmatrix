package com.das.skillmatrix.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.Team;
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

    public boolean checkCareerAccess(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Career career = careerRepository.findById(careerId).orElse(null);
        return career != null && isCareerManager(user, career);
    }

    public boolean checkDepartmentAccess(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Department dept = departmentRepository.findById(departmentId).orElse(null);
        if (dept == null)
            return false;
        if (isCareerManager(user, dept.getCareer()))
            return true;
        return isDepartmentManager(user, dept);
    }

    public boolean checkTeamAccess(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null)
            return false;
        if (isCareerManager(user, team.getDepartment().getCareer()))
            return true;
        if (isDepartmentManager(user, team.getDepartment()))
            return true;
        return isTeamManager(user, team);
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

    private boolean isCareerManager(User user, Career career) {
        return user != null && career != null
                && career.getManagers().stream().anyMatch(u -> u.getUserId().equals(user.getUserId()));
    }

    private boolean isDepartmentManager(User user, Department department) {
        return user != null && department != null
                && department.getManagers().stream().anyMatch(u -> u.getUserId().equals(user.getUserId()));
    }

    public boolean canManageDepartment(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Department dept = departmentRepository.findById(departmentId).orElse(null);
        if (dept == null)
            return false;
        return isCareerManager(user, dept.getCareer());
    }

    public boolean canManageTeam(Long teamId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null)
            return false;
        if (isCareerManager(user, team.getDepartment().getCareer()))
            return true;
        return isDepartmentManager(user, team.getDepartment());
    }

    public boolean canMoveDepartment(Long sourceCareerId, Long targetCareerId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        return checkCareerAccess(sourceCareerId) && checkCareerAccess(targetCareerId);
    }

    public boolean canViewDepartmentList(Long careerId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        if (checkCareerAccess(careerId))
            return true;
        return departmentRepository.existsByCareer_CareerIdAndManagers_UserId(careerId, user.getUserId());
    }

    public boolean canViewDepartmentDetail(Long departmentId) {
        User user = getCurrentUser();
        if (isAdmin(user))
            return true;
        Department dept = departmentRepository.findById(departmentId).orElse(null);
        if (dept == null)
            return false;
        return canViewDepartmentList(dept.getCareer().getCareerId());
    }

    private boolean isTeamManager(User user, Team team) {
        return user != null && team != null
                && team.getManagers().stream().anyMatch(u -> u.getUserId().equals(user.getUserId()));
    }
}
