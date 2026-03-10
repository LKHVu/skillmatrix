package com.das.skillmatrix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, QuerydslPredicateExecutor<Team> {
    long countByDepartment_DepartmentId(Long departmentId);

    boolean existsByNameIgnoreCaseAndDepartment_DepartmentIdAndStatusIn(
            String name,
            Long departmentId,
            List<GeneralStatus> statuses);

    @Query("""
                SELECT COUNT(t) > 0 FROM Team t
                WHERE LOWER(t.name) = LOWER(:name)
                  AND t.department.departmentId = :departmentId
                  AND t.teamId != :excludeTeamId
                  AND t.status IN :statuses
            """)
    boolean existsByNameAndDepartmentIdExcluding(
            @Param("name") String name,
            @Param("departmentId") Long departmentId,
            @Param("excludeTeamId") Long excludeTeamId,
            @Param("statuses") List<GeneralStatus> statuses);
    List<Team> findByStatusAndDeletedAtBefore(GeneralStatus status, LocalDateTime cutoff);    
}