package com.das.skillmatrix.service.helper;

import java.time.LocalDateTime;
import java.util.List;

import com.das.skillmatrix.dto.request.criteria.TeamSearchCriteria;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.QTeam;
import com.querydsl.core.BooleanBuilder;

public class TeamSearchHelper {
    private static final QTeam team = QTeam.team;

    // Build global search query with team, department, and career names
    public static BooleanBuilder buildGlobalSearch(List<Long> departmentIds, TeamSearchCriteria criteria) {
        BooleanBuilder builder = new BooleanBuilder();
        applyStatuses(builder, criteria.getStatus());
        applyDateFilter(builder, criteria.getDateFilter());
        if (departmentIds != null && !departmentIds.isEmpty()) {
            builder.and(team.department.departmentId.in(departmentIds));
        }
        String keyword = trimKeyword(criteria.getKeyword());
        if (keyword != null) {
            builder.and(
                team.name.containsIgnoreCase(keyword)
                .or(team.department.name.containsIgnoreCase(keyword))
                .or(team.department.career.name.containsIgnoreCase(keyword))
            );
        }
        return builder;
    }

    // Build search query with team name
    public static BooleanBuilder buildDepartmentSearch(TeamSearchCriteria criteria) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(team.department.departmentId.eq(criteria.getDepartmentId()));
        applyStatuses(builder, criteria.getStatus());
        applyDateFilter(builder, criteria.getDateFilter());
        String keyword = trimKeyword(criteria.getKeyword());
        if (keyword != null) {
            builder.and(team.name.containsIgnoreCase(keyword));
        }
        return builder;
    }

    private static void applyStatuses(BooleanBuilder builder, String statusFilter) {
        if ("ACTIVE".equals(statusFilter)) {
            builder.and(team.status.eq(GeneralStatus.ACTIVE));
        } else if ("DEACTIVE".equals(statusFilter)) {
            builder.and(team.status.eq(GeneralStatus.DEACTIVE));
        } else {
            builder.and(team.status.in(GeneralStatus.ACTIVE, GeneralStatus.DEACTIVE));
        }
    }
    
    private static void applyDateFilter(BooleanBuilder builder, String dateFilter) {
        if (dateFilter == null) return;
        LocalDateTime now = LocalDateTime.now();
        switch (dateFilter) {
            case "LAST_7_DAYS" -> builder.and(team.createdAt.goe(now.minusDays(7)));
            case "LAST_30_DAYS" -> builder.and(team.createdAt.goe(now.minusDays(30)));
            case "THIS_YEAR" -> builder.and(team.createdAt.goe(
                LocalDateTime.of(now.getYear(), 1, 1, 0, 0)));
            case "LAST_YEAR" -> builder.and(
                team.createdAt.goe(LocalDateTime.of(now.getYear() - 1, 1, 1, 0, 0))
                .and(team.createdAt.loe(LocalDateTime.of(now.getYear() - 1, 12, 31, 23, 59, 59))));
            default -> { }
        }
    }
    
    private static String trimKeyword(String keyword) {
        return (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
    }
}
