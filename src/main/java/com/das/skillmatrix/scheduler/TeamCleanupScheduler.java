package com.das.skillmatrix.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.repository.TeamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamCleanupScheduler {
    private final TeamRepository teamRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void hardDeleteTeamsAfter30Days() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        var candidates = teamRepository.findByStatusAndDeletedAtBefore(GeneralStatus.DELETED, cutoff);
        if (candidates.isEmpty()) return;
        candidates.forEach(teamRepository::delete);
        log.info("Hard deleted {} teams after 30 days", candidates.size());
    }
}