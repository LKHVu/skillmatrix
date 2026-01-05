package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.UserUpskillProgress;

@Repository
public interface UserUpskillProgressRepository extends JpaRepository<UserUpskillProgress, Long> {
    
}
