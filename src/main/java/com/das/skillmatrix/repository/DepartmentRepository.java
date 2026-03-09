package com.das.skillmatrix.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.dto.response.DepartmentBrief;
import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    long countByCareer_CareerId(Long careerId);
    List<Department> findByCareer_CareerId(Long careerId);
    boolean existsByCareer_CareerIdAndManagers_UserId(Long careerId, Long userId);
    boolean existsByNameIgnoreCaseAndCareer_CareerId(String name, Long careerId);
    boolean existsByDepartmentIdAndManagers_UserId(Long departmentId, Long userId);
    @Query("SELECT d.career.careerId FROM Department d WHERE d.departmentId = :departmentId")
    Optional<Long> findCareerIdByDepartmentId(Long departmentId);
    List<Department> findByStatusAndDeletedAtBefore(GeneralStatus status, LocalDateTime dateTime);
    @Query("""
            select new com.das.skillmatrix.dto.response.DepartmentBrief(
                d.departmentId,
                d.name
            )
            from Department d
            where d.career.careerId = :careerId
            and d.status != com.das.skillmatrix.entity.GeneralStatus.DELETED
            """)
    List<DepartmentBrief> findDepartmentBriefsByCareerId(Long careerId);
    @Query("""
            select new com.das.skillmatrix.dto.response.DepartmentResponse(
                d.departmentId,
                d.name,
                d.description,
                d.career.careerId,
                d.status
            )
            from Department d
            where d.career.careerId = :careerId
            and d.status != com.das.skillmatrix.entity.GeneralStatus.DELETED
            """)
    Page<DepartmentResponse> findDepartmentResponsesByCareerId(Long careerId, Pageable pageable);
}
