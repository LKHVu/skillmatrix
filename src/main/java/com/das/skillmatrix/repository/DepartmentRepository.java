package com.das.skillmatrix.repository;

import com.das.skillmatrix.entity.Department;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    long countByCareer_CareerId(Long careerId);
    List<Department> findByCareer_CareerId(Long careerId);
}