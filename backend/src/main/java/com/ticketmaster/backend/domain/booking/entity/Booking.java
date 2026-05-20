package com.ticketmaster.backend.domain.booking.entity;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.common.BaseEntity;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 예매(Booking) - 사용자가 좌석을 잡은 단위
 *
 * - User : Booking = 1:N (한 사용자가 여러 예매 보유)
 * - Match : Booking = 1:N (한 회차에 여러 예매 발생)
 * - Booking : BookingSeat = 1:N (한 예매에 여러 좌석 포함 가능)
 *
 * 상태 흐름:
 * PENDING → CONFIRMED / CANCELED / EXPIRED
 * CONFIRMED → CANCELED
 *
 * ※ Booking은 "좌석에 대한 권리", Payment는 "돈 거래" — 별개 도메인
 */
@Getter
@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_booking_user_status", columnList = "user_id,status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(name = "booking_seq", sequenceName = "BOOKING_SEQ", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "booking_number", nullable = false, unique = true, length = 30)
    private String bookingNumber;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    /** 예매 취소 시각 (취소된 경우에만 값 존재) */
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    /** 취소 사유 (TK-21 Phase 0 마이그레이션에서 추가) */
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    /** 낙관적 락 — 자동 만료 스케줄러와 사용자 결제 confirm 동시 발생 시 lost update 방지 */
    @Version
    private Long version;

    /** 한 예매에 여러 좌석 (1:N, 양방향) */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    // ===== 생성 =====

    /**
     * 예매 생성 팩토리 메서드
     * BookingService.createBooking() 에서 호출
     */
    public static Booking create(User user, Match match, String bookingNumber, int totalPrice) {
        Booking b = new Booking();
        b.user = user;
        b.match = match;
        b.bookingNumber = bookingNumber;
        b.totalPrice = totalPrice;
        b.status = BookingStatus.PENDING;
        return b;
    }

    // ===== 상태 변경 =====

    /** 예매 확정 (결제 성공 시 호출) - PENDING → CONFIRMED */
    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_PENDING);
        }
        this.status = BookingStatus.CONFIRMED;
    }

    /** 예매 취소 (사용자 / 관리자 취소 공용 — 취소 사유 포함) */
    public void cancel(String reason) {
        if (this.status == BookingStatus.CANCELED) {
            throw new BusinessException(ErrorCode.BOOKING_ALREADY_CANCELED);
        }
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }
        this.status = BookingStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    /** 예매 취소 (하위 호환 — 관리자/스케줄러용) */
    public void cancel() {
        if (this.status != BookingStatus.PENDING && this.status != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }
        this.status = BookingStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    /** 결제 시간 초과 자동 만료 — PENDING → EXPIRED */
    public void expire() {
        if (this.status != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_PENDING);
        }
        this.status = BookingStatus.EXPIRED;
        this.canceledAt = LocalDateTime.now();
    }

    /** 좌석 추가 */
    public void addBookingSeat(BookingSeat bookingSeat) {
        this.bookingSeats.add(bookingSeat);
        bookingSeat.assignBooking(this);
    }
}