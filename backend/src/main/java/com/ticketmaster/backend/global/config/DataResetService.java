package com.ticketmaster.backend.global.config;

import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.booking.repository.BookingSeatRepository;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시드 데이터 reset 전용 서비스 — SeedData 와 분리되어 자체 트랜잭션(REQUIRES_NEW) 사용
 * <p>
 * 분리 이유:
 * SeedData.run() 의 단일 트랜잭션 안에서 reset + 시드 생성을 묶으면, reset 후 commit 전에
 * PendingBookingExpirationScheduler 같은 백그라운드 잡이 옛 PENDING booking 을 보고
 * UPDATE 시도하면서 같은 row 의 락을 동시에 잡으려 해 데드락(ORA-00060) 발생.
 * reset 만 REQUIRES_NEW 로 분리해 즉시 commit 시키면 스케줄러가 빈 테이블을 보게 되어 충돌 회피.
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
public class DataResetService {

    private final PaymentRepository paymentRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final QueueRepository queueRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final EventRepository eventRepository;

    /**
     * 전체 테이블 reset — FK 자식 → 부모 순서로 deleteAllInBatch
     * REQUIRES_NEW 로 호출자 트랜잭션과 분리되어 즉시 commit 됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset() {
        paymentRepository.deleteAllInBatch();
        bookingSeatRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        queueRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }
}
