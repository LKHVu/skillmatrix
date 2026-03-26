package com.das.skillmatrix.repository.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.das.skillmatrix.dto.request.TeamFilterRequest;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;

import jakarta.persistence.criteria.Predicate;

public class TeamSpecification {

    public static Specification<Team> filterTeams(
            List<Long> departmentIds,
            TeamFilterRequest request
    ) {
        return (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            // 1. Scope
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("department").get("departmentId").in(departmentIds));
            }

            // 2. Status filter
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            } else {
                predicates.add(root.get("status").in(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
            }

            // 3. Keyword search
            if (StringUtils.hasText(request.getKeyword())) {
                String keywordPattern = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), keywordPattern);
                Predicate deptNamePredicate = cb.like(
                        cb.lower(root.get("department").get("name")), keywordPattern);
                Predicate careerNamePredicate = cb.like(
                        cb.lower(root.get("department").get("career").get("name")), keywordPattern);
                predicates.add(cb.or(namePredicate, deptNamePredicate, careerNamePredicate));
            }

            // 4. Date Modified filter
            applyDateFilter(predicates, request.getDateModified(), root, cb);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Team> filterTeamsByDepartment(
            Long departmentId,
            TeamFilterRequest request
    ) {
        return (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("department").get("departmentId"), departmentId));

            // Status
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            } else {
                predicates.add(root.get("status").in(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
            }

            // Keyword
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            // Date Modified filter
            applyDateFilter(predicates, request.getDateModified(), root, cb);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applyDateFilter(
            ArrayList<Predicate> predicates,
            String dateModified,
            jakarta.persistence.criteria.Root<Team> root,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        if (!StringUtils.hasText(dateModified)) return;

        LocalDateTime start = null;
        LocalDateTime end = null;
        LocalDateTime now = LocalDateTime.now();

        switch (dateModified.toLowerCase()) {
            case "last 7 days":
                start = now.minusDays(7);
                break;
            case "last 30 days":
                start = now.minusDays(30);
                break;
            case "this year":
                start = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
                end = LocalDateTime.of(now.getYear(), 12, 31, 23, 59, 59);
                break;
            case "last year":
                start = LocalDateTime.of(now.getYear() - 1, 1, 1, 0, 0);
                end = LocalDateTime.of(now.getYear() - 1, 12, 31, 23, 59, 59);
                break;
            case "any time":
            default:
                break;
        }

        if (start != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (end != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
        }
    }
}
