package com.ticketmaster.backend.domain.queue.entity;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실시간 제어는 Redis. 이 테이블은 이력/최종 상태 보관용
 */
@Getter
@Entity
@Table(name = "queues",
        indexes = {
                @Index(name = "idx_queue_match_status", columnList = "match_id,status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Queue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "queue_seq")
    @SequenceGenerator(name = "queue_seq", sequenceName = "QUEUE_SEQ", allocationSize = 50)
    private Long id;

    /** 한 사용자가 여러 대기열에 입장 가능 (대회별/재시도별로 레코드 누적) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 한 회차에 수많은 사용자가 대기열을 탐 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "queue_token", nullable = false, length = 100, unique = true)
    private String queueToken;

    @Column(name = "queue_number", nullable = false)
    private long queueNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "allowed_at")
    private LocalDateTime allowedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 새 대기열 진입 이력을 만드는 팩토리 메서드
     * new 대신 이 메서드로만 만들게 해서, 필수 필드가 빠진 채로 객체가 생기는 걸 방지
     */
    public static Queue createWaiting(
            User user,
            Match match,
            String queueToken,
            long queueNumber,
            LocalDateTime enteredAt,
            LocalDateTime expiresAt
    ) {
        Queue q = new Queue();
        q.user = user;
        q.match = match;
        q.queueToken = queueToken;
        q.queueNumber = queueNumber;
        q.status = QueueStatus.WAITING;
        q.enteredAt = enteredAt;
        q.expiresAt = expiresAt;
        return q;
    }

    /**
     * 대기열 상태를 ALLOWED 로 전환
     * 스케줄러가 200명 단위로 승격할 때 호출
     */
    public void markAllowed(LocalDateTime allowedAt) {
        this.status = QueueStatus.ALLOWED;
        this.allowedAt = allowedAt;
    }

    /**
     * 대기열 상태를 EXPIRED 로 전환
     * 결제 완료 후 admission 회수 시 호출 — "결제 완료 = 1회 예매 종료" 원칙 (다음 예매는 다시 대기열)
     */
    public void markExpired() {
        this.status = QueueStatus.EXPIRED;
    }
}
