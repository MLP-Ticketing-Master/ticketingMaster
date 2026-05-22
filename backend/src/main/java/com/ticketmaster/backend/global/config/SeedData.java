package com.ticketmaster.backend.global.config;

import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("dev") // 개발 환경에서만 실행
public class SeedData implements CommandLineRunner {
    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final QueueRepository queueRepository;
    private final PaymentRepository paymentRepository;

    private Match match1, match2, match3, match4;
    private SeatGrade VIPGrade, RGrade, SGrade;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        // 실행전 테이블 초기화 코드
        resetData();

        // 날짜 기준점
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 1. Event 생성 (오늘 포함, 넉넉하게 -30일 ~ +60일)
        Event event = createEvent(today.minusDays(30), today.plusDays(60));
        eventRepository.save(event);

        // 2. Team 생성
        Team homeTeam = createTeam("T1");
        Team awayTeam = createTeam("Gen.G");
        teamRepository.save(homeTeam);
        teamRepository.save(awayTeam);

        // 3. Match 4개 생성
        // Case 1: 예매 시작 전 (예매 오픈: +7일, 경기: +21일)
        match1 = createMatch(event, homeTeam, awayTeam,
                "4경기",
                today.plusDays(21),
                now.plusDays(21).withHour(17).withMinute(0),
                now.plusDays(21).withHour(21).withMinute(0),
                now.plusDays(7),                      // 예매 시작: +7일
                now.plusDays(20).withHour(23).withMinute(59), // 예매 종료: 경기 전날
                now.plusDays(20).withHour(17).withMinute(0)   // 취소 가능: 경기 전날 17시
        );

        // Case 2: 예매 진행 중 (예매 오픈: -1일, 경기: +7일)
        match2 = createMatch(event, homeTeam, awayTeam,
                "3경기",
                today.plusDays(7),
                now.plusDays(7).withHour(17).withMinute(0),
                now.plusDays(7).withHour(21).withMinute(0),
                now.minusDays(1),                     // 예매 시작: 어제
                now.plusDays(6).withHour(23).withMinute(59),  // 예매 종료: 경기 전날
                now.plusDays(6).withHour(17).withMinute(0)    // 취소 가능: 경기 전날 17시
        );

        // Case 3: 예매 마감, 경기 전 (예매 오픈: -14일, 예매 종료: -1일, 경기: +1일)
        match3 = createMatch(event, homeTeam, awayTeam,
                "2경기",
                today.plusDays(1),
                now.plusDays(1).withHour(17).withMinute(0),
                now.plusDays(1).withHour(21).withMinute(0),
                now.minusDays(14),                    // 예매 시작: -14일
                now.minusDays(1).withHour(23).withMinute(59), // 예매 종료: 어제
                now.minusDays(1).withHour(17).withMinute(0)   // 취소 가능: 어제 17시
        );

        // Case 4: 예매 마감, 경기 종료 (예매 오픈: -21일, 경기: -7일)
        match4 = createMatch(event, homeTeam, awayTeam,
                "1경기",
                today.minusDays(7),
                now.minusDays(7).withHour(17).withMinute(0),
                now.minusDays(7).withHour(21).withMinute(0),
                now.minusDays(21),                    // 예매 시작: -21일
                now.minusDays(8).withHour(23).withMinute(59), // 예매 종료: 경기 전날
                now.minusDays(8).withHour(17).withMinute(0)   // 취소 가능: 경기 전날 17시
        );

        matchRepository.saveAll(List.of(match1, match2, match3, match4));

        // 4. SeatGrade 생성
        VIPGrade = createSeatGrade(event, "VIP", 100000, "FF0000");
        RGrade = createSeatGrade(event, "R", 70000, "00FF00");
        SGrade = createSeatGrade(event, "S", 50000, "0000FF");
        seatGradeRepository.saveAll(List.of(VIPGrade, RGrade, SGrade));

        // 5. Section 생성 (Event 단위 — 매치 공유)
        Section sectionA = createSection(event, "A구역", 1, null);
        Section sectionB = createSection(event, "B구역", 2, null);
        Section sectionC = createSection(event, "C구역", 3, null);
        Section sectionD = createSection(event, "D구역", 4, null);
        sectionRepository.saveAll(List.of(sectionA, sectionB, sectionC, sectionD));

        // 6. Seat 생성 (Match 단위 — 매치마다 독립적인 좌석)
        for (Match match : List.of(match1, match2, match3, match4)) {
            for (Section section : List.of(sectionA, sectionB, sectionC, sectionD)) {
                createSeat(match, section);
            }
        }

        log.info("[SeedData] ✅ 시드 데이터 삽입 완료!");
    }

    /**
     * Event 엔티티 생성 및 반환 메소드
     */
    private Event createEvent(LocalDate startDate, LocalDate endDate) {
        return Event.builder()
                .title("2026 LCK Spring 결승전")
                .sportType(SportType.LOL)
                .place("LOL Park")
                .thumbnailUrl("lck_thumb.png")
                .detailImageUrl("lck_detail.png")
                .description("2026 LoL 챔피언스 코리아 스프링 시즌의 대미를 장식할 결승전!")
                .startDate(startDate)
                .endDate(endDate)
                .matchDurationText("약 240분")
                .ageRating("12세 이용가")
                .bookingNotice("1인당 최대 2매 예매 가능합니다.")
                .maxTicketsPerUser(2)
                .cancelFee(1000)
                .status(EventStatus.OPEN)
                .build();
    }

    /**
     * Team 엔티티 생성 및 반환 메소드
     */
    private Team createTeam(String name) {
        return Team.builder()
                .name(name)
                .logoImageUrl(name.toLowerCase().replace('.', ' ').strip() + "_logo.png")
                .sportType(SportType.LOL)
                .build();
    }

    /**
     * Match 엔티티 생성 및 반환 메소드
     */
    private Match createMatch(
            Event event, Team homeTeam, Team awayTeam,
            String roundLabel,
            LocalDate matchDate,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime bookingOpenAt,
            LocalDateTime bookingCloseAt,
            LocalDateTime cancelAvailableUntil
    ) {
        return Match.builder()
                .event(event)
                .roundLabel(roundLabel)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .matchDate(matchDate)
                .startAt(startAt)
                .endAt(endAt)
                .bookingOpenAt(bookingOpenAt)
                .bookingCloseAt(bookingCloseAt)
                .cancelAvailableUntil(cancelAvailableUntil)
                .status(resolveMatchStatus(startAt, endAt)) // 자동 결정
                .build();
    }

    /**
     * SeatGrade 엔티티 생성 및 반환 메소드
     */
    private SeatGrade createSeatGrade(Event event, String gradeCode, int price, String colorHex) {
        return SeatGrade.create(event, gradeCode, price, colorHex);
    }

    /**
     * Section 엔티티 생성 및 반환 메소드
     */
    private Section createSection(Event event, String name, int displayOrder, String description) {
        return Section.create(event, name, displayOrder, description);
    }

    /**
     * 구역별 좌석 생성 메소드 (100석 씩)
     */
    public void createSeat(Match match, Section section) {
        List<Seat> seatList = new ArrayList<>();

        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 10; c++) {
                SeatGrade seatGrade = switch (r) {
                    case 1, 2    -> VIPGrade;
                    case 3, 4, 5 -> RGrade;
                    default      -> SGrade;
                };

                Seat seat = Seat.create(
                        match,
                        section,
                        seatGrade,
                        Character.toString((char) ('A' + (r - 1))),
                        c,
                        createSeatCode(seatGrade.getGradeCode(), section.getName(),
                                Character.toString((char) ('A' + (r - 1))), c)
                );
                seatList.add(seat);
            }
        }

        seatRepository.saveAll(seatList);
        log.info("[SeedData] {} - {} 좌석 {}개 생성 완료", match.getRoundLabel(), section.getName(), seatList.size());
    }

    /**
     * 좌석 코드 생성기 ("구역-열+번호(등급)" ex: "A구역-D7(VIP)")
     */
    private String createSeatCode(String seatGrade, String seatSection, String rowLabel, Integer seatNo) {
        return seatSection + "-" + rowLabel + seatNo.toString() + "(" + seatGrade + ")";
    }

    /**
     * 전체 테이블 데이터 초기화 메소드
     */
    private void resetData() {
        /**
         * 삭제 순서 (FK 자식 → 부모)
         * payment -> booking(+booking_seat cascade) -> queue -> seat -> section & seat_grade -> match -> team & event
         * Booking 만 deleteAll() 로 호출해서 cascade=ALL + orphanRemoval 로 booking_seats 자동 정리
         * 나머지는 자식이 먼저 정리됐으므로 deleteAllInBatch() 로 일괄 삭제
         */
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAll();
        queueRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    /**
     * 현재 시각 기준으로 MatchStatus 자동 결정
     * - now < startAt         → SCHEDULED (경기 예정)
     * - startAt <= now < endAt → LIVE     (경기 진행 중)
     * - endAt <= now           → FINISHED (경기 종료)
     */
    private MatchStatus resolveMatchStatus(LocalDateTime startAt, LocalDateTime endAt) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startAt)) {
            return MatchStatus.SCHEDULED;
        } else if (now.isBefore(endAt)) {
            return MatchStatus.LIVE;
        } else {
            return MatchStatus.FINISHED;
        }
    }
}
