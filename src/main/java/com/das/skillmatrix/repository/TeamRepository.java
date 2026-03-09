package com.das.skillmatrix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @EntityGraph(attributePaths = { "managers", "department" })
    Page<Team> findAll(Pageable pageable);
    long countByDepartment_DepartmentId(Long departmentId);
    List<Team> findByDepartment_DepartmentId(Long departmentId);
    boolean existsByTeamIdAndManagers_UserId(Long teamId, Long userId);
    @Query("SELECT t.department.departmentId FROM Team t WHERE t.teamId = :teamId")
    Optional<Long> findDepartmentIdByTeamId(Long teamId);
    @Query("SELECT t.department.career.careerId FROM Team t WHERE t.teamId = :teamId")
    Optional<Long> findCareerIdByTeamId(Long teamId);
}