package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.UserSkillEvaluation;

@Repository
public interface UserSkillEvaluationRepository extends JpaRepository<UserSkillEvaluation, Long> {
    
}
