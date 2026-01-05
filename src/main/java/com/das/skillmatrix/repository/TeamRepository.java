package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
}
