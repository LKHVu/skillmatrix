package com.das.skillmatrix.repository;

import com.das.skillmatrix.dto.response.CareerResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.CareerStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long> {

    boolean existsByNameIgnoreCase(String name);
    Optional<Career> findByCareerIdAndStatusIn(Long careerId, List<CareerStatus> statuses);
    Optional<Career> findByCareerIdAndStatus(Long careerId, CareerStatus status);
    @Query("""
        select new com.das.skillmatrix.dto.response.CareerResponse(
            c.careerId,
            c.name,
            c.description,
            c.status
        )
        from Career c
        where c.status in (com.das.skillmatrix.entity.CareerStatus.ACTIVE,
                        com.das.skillmatrix.entity.CareerStatus.DEACTIVE)
    """)
    Page<CareerResponse> findCareerResponses(Pageable pageable);
    List<Career> findByStatusAndDeletedAtBefore(CareerStatus status, LocalDateTime cutoff);
}