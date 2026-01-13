package com.das.skillmatrix.scheduler;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentCleanupScheduler {

    private final DepartmentRepository departmentRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void hardDeleteDepartmentsAfter30Days() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        var candidates = departmentRepository.findByStatusAndDeletedAtBefore(GeneralStatus.DELETED, cutoff);
        if (candidates.isEmpty()) return;
        candidates.forEach(departmentRepository::delete);
    }
}