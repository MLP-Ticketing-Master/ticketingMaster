package com.ticketmaster.backend.domain.queue.scheduler;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.queue.service.QueueAdmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 설정 주기(admission-interval-seconds, 기본 10초)마다 활성 매치별로 대기열 승격을 트리거함
 * 단일 인스턴스 환경 가정 (멀티 인스턴스 시 ShedLock 같은 분산 락 필요 — 현재 범위 외)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueAdmissionScheduler {

    private final MatchRepository matchRepository;
    private final QueueAdmissionService queueAdmissionService;

    @Scheduled(
            fixedDelayString = "${queue.admission-interval-seconds}",
            timeUnit = TimeUnit.SECONDS
    )
    public void promote() {
        LocalDateTime now = LocalDateTime.now();
        // 현재 예매 가능 기간 안에 있는 매치 전체를 가져와서 매치별로 따로 처리
        List<Match> active = matchRepository.findActiveMatchesForQueueAdmission(now);
        for (Match match : active) {
            try {
                queueAdmissionService.promoteForMatch(match.getId(), now);
            } catch (Exception e) {
                // 한 매치 실패가 다른 매치 처리를 막지 않도록 try-catch 로 격리
                log.error("[QueueScheduler] matchId={} 승격 실패", match.getId(), e);
            }
        }
    }
}
