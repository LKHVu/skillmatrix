package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.das.skillmatrix.entity.BusinessChangeLog;

public interface BusinessChangeLogRepository extends JpaRepository<BusinessChangeLog, Long> {
}