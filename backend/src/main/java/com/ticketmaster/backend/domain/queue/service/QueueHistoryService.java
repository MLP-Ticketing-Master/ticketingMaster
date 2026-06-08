package com.ticketmaster.backend.domain.queue.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

// 진입 이력 배치 저장 - 버퍼 적재 + 주기적 flush
// 큐 순서/권한 기준은 Redis, 이 이력은 감사/보정용 기록
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueHistoryService {

    private final QueueHistoryWriter writer;

    // 진입 이력 임시 보관함 - 여러 톰캣 스레드가 동시에 넣어도 안전한 큐(락 없이 동작)
    private final ConcurrentLinkedQueue<QueueHistoryRecord> buffer = new ConcurrentLinkedQueue<>();

    // 현재 쌓인 건수 - 큐의 size() 는 매번 전체를 세서 느리므로 카운터로 따로 셈
    private final AtomicInteger pending = new AtomicInteger();

    // 버퍼에 담을 수 있는 최대 건수 - 넘으면 바로 비워서 메모리 폭주 막음
    @Value("${queue.history.buffer-max:20000}")
    private int bufferMax;

    // 한 번에 비우는 최대 건수 - 트랜잭션 하나가 너무 커지지 않게 제한
    @Value("${queue.history.flush-max:2000}")
    private int flushMax;

    // 진입 직후 불리는 빠른 경로 - DB 안 건드리고 메모리 큐에 넣기만 함
    // 너무 많이 쌓이면(상한 초과) 그 자리에서 바로 비워 메모리 보호
    public void enqueueWaitingHistory(QueueHistoryRecord record) {
        buffer.offer(record);
        if (pending.incrementAndGet() >= bufferMax) {
            flush();
        }
    }

    // 주기적 배치 저장 - 버퍼 비워서 모은 뒤 별도 빈에서 한 트랜잭션 saveAll
    // 저장 실패해도 진입(Redis)엔 영향 없음 - 로그만 남기고 삼킴
    @Scheduled(fixedDelayString = "${queue.history.flush-interval-ms:500}")
    public void flush() {
        List<QueueHistoryRecord> batch = new ArrayList<>();
        QueueHistoryRecord r;
        while (batch.size() < flushMax && (r = buffer.poll()) != null) {
            batch.add(r);
            pending.decrementAndGet();
        }
        if (batch.isEmpty()) {
            return;
        }
        try {
            writer.saveBatch(batch);
        } catch (Exception e) {
            log.error("[Queue] 이력 배치 저장 실패 size={}", batch.size(), e);
        }
    }

    // 종료 시 잔여분 마지막 flush
    @PreDestroy
    public void flushOnShutdown() {
        log.info("[Queue] 종료 전 이력 flush pending={}", pending.get());
        while (!buffer.isEmpty()) {
            flush();
        }
    }
}
