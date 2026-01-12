package com.das.skillmatrix.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
@EntityGraph(attributePaths = {"manager", "department"})
    Page<Team> findAll(Pageable pageable);
}
