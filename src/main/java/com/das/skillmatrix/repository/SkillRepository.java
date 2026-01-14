package com.das.skillmatrix.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Skill;
import com.das.skillmatrix.entity.SkillStatus;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    boolean existsByNameIgnoreCase(String name);
    Skill findByNameIgnoreCase(String name);
    Page<Skill> findByStatusIn(List<SkillStatus> status, Pageable pageable);
}
