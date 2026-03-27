package com.das.skillmatrix.repository.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.das.skillmatrix.dto.request.UserFilterRequest;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.TeamMember;
import com.das.skillmatrix.entity.User;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class UserSpecification {

    public static Specification<User> filterUsers(UserFilterRequest filter, 
                                                 List<Long> scopeIds, 
                                                 String currentUserRole) {
        return (root, query, cb) -> {
            // Use distinct to avoid duplicates due to joins
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            // 1. Keyword search (email, fullName, userId, team name)
            if (StringUtils.hasText(filter.getKeyword())) {
                String likePattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), likePattern);
                Predicate namePredicate = cb.like(cb.lower(root.get("fullName")), likePattern);
                
                // Search by userId (cast Long to String for LIKE)
                Predicate userIdPredicate = cb.like(
                    root.get("userId").as(String.class), likePattern);
                
                // Search by Team Name (via TeamMember subquery)
                Subquery<Long> teamNameSubquery = query.subquery(Long.class);
                Root<TeamMember> tmNameRoot = teamNameSubquery.from(TeamMember.class);
                Join<TeamMember, User> tmNameUser = tmNameRoot.join("user");
                Join<TeamMember, Team> tmNameTeam = tmNameRoot.join("team");
                teamNameSubquery.select(tmNameUser.get("userId"))
                    .where(cb.like(cb.lower(tmNameTeam.get("name")), likePattern));
                
                Predicate teamNamePredicate = root.get("userId").in(teamNameSubquery);
                
                predicates.add(cb.or(emailPredicate, namePredicate, userIdPredicate, teamNamePredicate));
            }

            // 2. Status filter
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            } else {
                // By default, do not show hard-deleted users if they exist, but here we just
                // return ACTIVE and DEACTIVE, not DELETED. 
                predicates.add(root.get("status").in(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
            }

            // 3. Created date filter
            applyDateFilter(root, cb, predicates, filter.getDateModified());

            // 4. Team ID filter (when viewing users of a specific team)
            if (filter.getTeamId() != null) {
                // Need to join through TeamMember since users can belong to multiple teams
                Subquery<Long> teamSubquery = query.subquery(Long.class);
                Root<TeamMember> teamMemberRoot = teamSubquery.from(TeamMember.class);
                Join<TeamMember, User> tmUserJoin = teamMemberRoot.join("user");
                Join<TeamMember, Team> tmTeamJoin = teamMemberRoot.join("team");
                
                teamSubquery.select(tmUserJoin.get("userId"))
                           .where(cb.equal(tmTeamJoin.get("teamId"), filter.getTeamId()));
                
                predicates.add(root.get("userId").in(teamSubquery));
            }

            // 5. Scope resolution based on currentUserRole and scopeIds
            applyScopeFilter(root, query, cb, predicates, scopeIds, currentUserRole);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applyDateFilter(Root<User> root, CriteriaBuilder cb, 
                                       List<Predicate> predicates, String dateModified) {
        if (!StringUtils.hasText(dateModified) || "any time".equalsIgnoreCase(dateModified.trim())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        switch (dateModified.trim().toLowerCase()) {
            case "last 7 days":
                startDate = now.minusDays(7);
                break;
            case "last 30 days":
                startDate = now.minusDays(30);
                break;
            case "this year":
                startDate = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
                endDate = LocalDateTime.of(now.getYear(), 12, 31, 23, 59, 59);
                break;
            case "last year":
                startDate = LocalDateTime.of(now.getYear() - 1, 1, 1, 0, 0);
                endDate = LocalDateTime.of(now.getYear() - 1, 12, 31, 23, 59, 59);
                break;
            default:
                return;
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }
    }

    private static void applyScopeFilter(Root<User> root, 
                                        jakarta.persistence.criteria.CriteriaQuery<?> query,
                                        CriteriaBuilder cb, 
                                        List<Predicate> predicates, 
                                        List<Long> scopeIds, 
                                        String currentUserRole) {
        
        if ("ADMIN".equals(currentUserRole) || scopeIds == null || scopeIds.isEmpty()) {
            // ADMIN sees everyone. If scopeIds is empty for a manager, they see no one
            if (!"ADMIN".equals(currentUserRole)) {
                predicates.add(cb.disjunction()); // 0 = 1 (always false) return empty
            }
            return;
        }

        switch (currentUserRole) {
            case "MANAGER_CAREER":
                // Can see users belonging to Departments within their managed Careers
                Join<User, Department> deptJoin = root.join("department", JoinType.LEFT);
                Join<Department, Career> careerJoin = deptJoin.join("career", JoinType.LEFT);
                predicates.add(careerJoin.get("careerId").in(scopeIds));
                break;

            case "MANAGER_DEPARTMENT":
                // Can see users belonging to Departments within the same Careers as their managed Departments
                Join<User, Department> userDeptJoin = root.join("department", JoinType.LEFT);
                Join<Department, Career> userCareerJoin = userDeptJoin.join("career", JoinType.LEFT);
                
                Subquery<Long> careerSubquery = query.subquery(Long.class);
                Root<Department> deptRoot = careerSubquery.from(Department.class);
                Join<Department, Career> deptCareerJoin = deptRoot.join("career");
                careerSubquery.select(deptCareerJoin.get("careerId"))
                             .where(deptRoot.get("departmentId").in(scopeIds));
                
                predicates.add(userCareerJoin.get("careerId").in(careerSubquery));
                break;

            case "MANAGER_TEAM":
                // Can see users belonging to Teams within the same Departments as their managed Teams
                // Users are linked to Teams via TeamMember
                Subquery<Long> teamMemberSubquery = query.subquery(Long.class);
                Root<TeamMember> tmRoot = teamMemberSubquery.from(TeamMember.class);
                Join<TeamMember, User> tmUser = tmRoot.join("user");
                Join<TeamMember, Team> tmTeam = tmRoot.join("team");
                Join<Team, Department> teamDept = tmTeam.join("department");
                
                Subquery<Long> deptSubquery = query.subquery(Long.class);
                Root<Team> teamRoot = deptSubquery.from(Team.class);
                Join<Team, Department> tDept = teamRoot.join("department");
                deptSubquery.select(tDept.get("departmentId"))
                           .where(teamRoot.get("teamId").in(scopeIds));
                           
                teamMemberSubquery.select(tmUser.get("userId"))
                                .where(teamDept.get("departmentId").in(deptSubquery));
                
                predicates.add(root.get("userId").in(teamMemberSubquery));
                break;

            case "STAFF":
                // Usually Staff can only see users in their own Teams
                Subquery<Long> staffTeamMemberSubq = query.subquery(Long.class);
                Root<TeamMember> slmRoot = staffTeamMemberSubq.from(TeamMember.class);
                Join<TeamMember, User> sTmUser = slmRoot.join("user");
                Join<TeamMember, Team> sTmTeam = slmRoot.join("team");
                
                staffTeamMemberSubq.select(sTmUser.get("userId"))
                                .where(sTmTeam.get("teamId").in(scopeIds));
                
                predicates.add(root.get("userId").in(staffTeamMemberSubq));
                break;
        }
    }
}
