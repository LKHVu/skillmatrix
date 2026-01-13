package com.das.skillmatrix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.dto.response.DepartmentResponse;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    long countByCareer_CareerId(Long careerId);
    List<Department> findByCareer_CareerId(Long careerId);
    boolean existsByCareer_CareerIdAndManagers_UserId(Long careerId, Long userId);
    boolean existsByNameIgnoreCaseAndCareer_CareerId(String name, Long careerId);
    List<Department> findByStatusAndDeletedAtBefore(GeneralStatus status, LocalDateTime dateTime);
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
    Page<DepartmentResponse> findDepartmentResponsesByCareerId(Long careerId, org.springframework.data.domain.Pageable pageable);
}