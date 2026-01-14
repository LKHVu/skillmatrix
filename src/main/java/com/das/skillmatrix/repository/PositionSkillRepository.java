package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.PositionSkill;

@Repository
public interface PositionSkillRepository extends JpaRepository<PositionSkill, Long> {
    boolean existsBySkill_SkillId(Long skillId);
}
