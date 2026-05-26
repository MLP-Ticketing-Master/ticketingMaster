package com.ticketmaster.backend.global.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 시드 데이터 reset 전용 서비스 — SeedData 와 분리되어 자체 트랜잭션(REQUIRES_NEW) 사용
 * <p>
 * REQUIRES_NEW 분리 이유:
 * SeedData.run() 의 단일 트랜잭션 안에서 reset + 시드 생성을 묶으면, reset 후 commit 전에
 * PendingBookingExpirationScheduler 같은 백그라운드 잡이 옛 PENDING booking 을 보고
 * UPDATE 시도하면서 같은 row 의 락을 동시에 잡으려 해 데드락(ORA-00060) 발생.
 * reset 만 REQUIRES_NEW 로 분리해 즉시 commit 시키면 스케줄러가 빈 테이블을 보게 되어 충돌 회피.
 * <p>
 * native DELETE 사용 이유:
 * Team/Match/Event/SeatGrade 엔티티의 @SQLRestriction("deleted_at IS NULL") 이
 * Repository deleteAllInBatch() 의 DELETE 쿼리에도 자동 부착돼 soft-deleted 행이 남음.
 * 그 결과 soft-deleted match 가 자식으로 남은 채 부모(teams) 삭제 시도 시 FK 위반(ORA-02292) 발생.
 * EntityManager native query 로 @SQLRestriction 우회하여 hard delete 처리.
 */
@Service
@Profile("dev")
public class DataResetService {

    @PersistenceContext
    private EntityManager em;

    /** FK 자식 → 부모 순서 (시드 reset 시 이 순서 유지 필수) */
    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
            "payments",
            "booking_seats",
            "bookings",
            "queues",
            "seats",
            "sections",
            "seat_grades",
            "matches",
            "teams",
            "events"
    );

    /** 전체 테이블 hard reset — REQUIRES_NEW 로 호출자 트랜잭션과 분리되어 즉시 commit 됨 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset() {
        for (String table : TABLES_IN_DELETE_ORDER) {
            em.createNativeQuery("DELETE FROM " + table).executeUpdate();
        }
    }
}
