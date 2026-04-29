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
}
