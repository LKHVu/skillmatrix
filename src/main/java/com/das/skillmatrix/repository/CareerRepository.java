package com.das.skillmatrix.repository;

import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.entity.Career;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long> {
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    @Query("""
        select new com.das.skillmatrix.dto.response.CareerResponse(
            c.careerId,
            c.name,
            c.description,
            count(d.departmentId)
        )
        from Career c
        left join c.departments d
        where c.deleted = false
        group by c.careerId, c.name, c.description
    """)
    Page<CareerResponse> findCareerResponses(Pageable pageable);
    Optional<Career> findByCareerIdAndDeletedFalse(Long careerId);
    Optional<Career> findFirstByNameIgnoreCase(String name);
}