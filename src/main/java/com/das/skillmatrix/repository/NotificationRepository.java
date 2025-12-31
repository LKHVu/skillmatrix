package com.das.skillmatrix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.das.skillmatrix.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
}
