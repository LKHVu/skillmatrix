package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.das.skillmatrix.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}