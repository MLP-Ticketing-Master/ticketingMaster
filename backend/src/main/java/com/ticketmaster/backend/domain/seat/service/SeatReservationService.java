package com.ticketmaster.backend.domain.seat.service;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReleaseResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReserveResponse;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 좌석 점유 / 해제 서비스
 *
 * 동시성 설계:
 *   - 좌석 상태는 DB에만 저장
 *   - JPA @Version 낙관적 락 — 트랜잭션 커밋 시점에 충돌 감지
 *   - 충돌 시 50~100ms 백오프 후 재시도 1~2회 (총 최대 3회)
 *   - 마지막 재시도도 실패 시 SEAT_ALREADY_RESERVED + conflictSeatIds 로 응답
 *
 * 트랜잭션 / 재시도 분리:
 *   - reserve()        : 재시도 루프만 — 트랜잭션 없음
 *   - tryReserveOnce() : @Transactional — 한 번의 점유 시도
 *   - self-injection 으로 self.tryReserveOnce() 호출 → AOP 프록시 경유로 트랜잭션 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReservationService {

    /** 점유 TTL — 7분 */
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(7);

    /** 재시도 최대 횟수 (최초 1회 포함) */
    private static final int MAX_ATTEMPTS = 3;

    /** 재시도 대기 시간 (ms). 충돌 분산을 위해 이 범위에서 랜덤 선택 */
    private static final long BACKOFF_MIN_MS = 50;
    private static final long BACKOFF_MAX_MS = 100;

    private final MatchRepository matchRepository;
    private final SeatRepository seatRepository;

    /**
     * @Transactional은 this.메서드()로 부르면 안 먹힘 (Spring 한계)
     * 그래서 자기 자신을 self로 주입받아 self.메서드()로 호출
     * @Lazy는 자기 주입 시 발생하는 오류 방지용
     */
    @Autowired
    @Lazy
    private SeatReservationService self;

    // -------- 점유 ----------------------------------------------

    /**
     * 좌석 일괄 점유 — 재시도 루프 진입점
     * <p>
     * 흐름:
     * 1. self.tryReserveOnce() 호출 (@Transactional)
     * 2. 정상 커밋 → 응답 반환
     * 3. OptimisticLockingFailureException → 50~100ms 백오프 후 재시도
     * 4. MAX_ATTEMPTS 모두 실패 → SEAT_ALREADY_RESERVED + 요청 seatIds 응답
     */
    public SeatReserveResponse reserve(Long matchId, Long userId, List<Long> seatIds) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return self.tryReserveOnce(matchId, userId, seatIds);
            } catch (OptimisticLockingFailureException e) {
                log.warn("[Reserve] 낙관적 락 충돌 attempt={}/{} matchId={} userId={} seatIds={}",
                        attempt, MAX_ATTEMPTS, matchId, userId, seatIds);

                if (attempt == MAX_ATTEMPTS) {
                    // 재시도 모두 실패 — 어떤 좌석이 충돌했는지 정확히 알 수 없으므로
                    // 요청한 seatIds 를 그대로 conflictSeatIds 로 반환
                    throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED, seatIds);
                }
                sleepBackoff();
            }
        }
        // 도달 불가능하지만 컴파일러용
        throw new IllegalStateException("unreachable");
    }

    /**
     * 한 번의 트랜잭션에서 점유 처리
     * 외부에서 직접 호출하지 말고 reserve() 를 통해 호출할 것
     */
    @Transactional
    public SeatReserveResponse tryReserveOnce(Long matchId, Long userId, List<Long> seatIds) {
        // 1. 매치 조회 (없으면 404)
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 2. maxTicketsPerUser 검증 (1인 당 2개까지 예매 가능)
        int maxTickets = match.getEvent().getMaxTicketsPerUser();
        if (seatIds.size() > maxTickets) {
            throw new BusinessException(ErrorCode.MAX_TICKETS_EXCEEDED);
        }

        // 3. 좌석 일괄 조회 (SeatGrade fetch join — totalPrice 계산용)
        List<Seat> seats = seatRepository.findByMatchAndIdIn(matchId, seatIds);
        if (seats.size() != seatIds.size()) {
            // 존재하지 않는 좌석 ID 포함 (또는 다른 매치 소속) → 404
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }

        // 4. 좌석 상태 검증
        //    - SOLD 1개라도 있으면 SEAT_ALREADY_SOLD + conflictSeatIds (즉시 거절)
        //    - 그 외 RESERVED 가 있으면 SEAT_ALREADY_RESERVED + conflictSeatIds
        List<Long> soldIds = seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.SOLD)
                .map(Seat::getId)
                .toList();
        if (!soldIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_SOLD, soldIds);
        }

        List<Long> reservedIds = seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.RESERVED)
                .map(Seat::getId)
                .toList();
        if (!reservedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED, reservedIds);
        }

        // 5. 점유 처리 (트랜잭션 커밋 시 @Version 체크 → 충돌 시 OptimisticLockingFailure)
        LocalDateTime until = LocalDateTime.now().plus(RESERVATION_TTL);
        int totalPrice = 0;
        List<Long> reservedSeatIds = new ArrayList<>(seats.size());

        for (Seat seat : seats) {
            seat.reserve(userId, until);
            totalPrice += seat.getSeatGrade().getPrice();
            reservedSeatIds.add(seat.getId());
        }

        return SeatReserveResponse.of(reservedSeatIds, until, totalPrice);
    }

    // -------- 해제 ----------------------------------------------

    /**
     * 본인 점유 좌석 해제. 멱등성:
     *  - 본인 점유 아닌 좌석은 응답에 미포함
     *  - 이미 해제 / 존재하지 않는 좌석도 응답에 미포함
     *  - 빈 리스트여도 200 OK
     *
     * 해제는 충돌이 거의 발생하지 않으므로 재시도 로직 없이 단일 @Transactional 로 처리
     */
    @Transactional
    public SeatReleaseResponse release(Long matchId, Long userId, List<Long> seatIds) {
        List<Seat> seats = seatRepository.findByMatchAndIdIn(matchId, seatIds);

        List<Long> released = new ArrayList<>(seats.size());
        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.RESERVED
                    && userId.equals(seat.getReservedBy())) {
                seat.releaseByOwner(userId);
                released.add(seat.getId());
            }
        }
        return SeatReleaseResponse.of(released);
    }

    // -------- 헬퍼 ----------------------------------------------

    /** 50~100ms 무작위 백오프 */
    private void sleepBackoff() {
        long delay = ThreadLocalRandom.current().nextLong(BACKOFF_MIN_MS, BACKOFF_MAX_MS + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
