package com.das.skillmatrix.scheduler;

import com.das.skillmatrix.entity.CareerStatus;
import com.das.skillmatrix.repository.CareerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareerCleanupScheduler {

    private final CareerRepository careerRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void hardDeleteCareersAfter30Days() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        var candidates = careerRepository.findByStatusAndDeletedAtBefore(CareerStatus.DELETED, cutoff);
        if (candidates.isEmpty()) return;
        candidates.forEach(careerRepository::delete);
    }
}