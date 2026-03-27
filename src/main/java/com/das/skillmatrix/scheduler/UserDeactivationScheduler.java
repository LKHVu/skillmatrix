package com.das.skillmatrix.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeactivationScheduler {

    private final UserRepository userRepository;

    @Transactional
    // Run daily at midnight (Asia/Ho_Chi_Minh timezone)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoReactivateExpiredUsers() {
        log.info("Starting scheduled task: autoReactivateExpiredUsers");
        
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = userRepository.reactivateExpiredUsers(now, GeneralStatus.ACTIVE);
        
        log.info("Finished scheduled task: autoReactivateExpiredUsers. Reactivated {} users.", updatedCount);
    }
}
