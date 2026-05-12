package com.ticketmaster.backend.global.config;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
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

    private Match match;
    private SeatGrade VIPGrade;
    private SeatGrade RGrade;
    private SeatGrade SGrade;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        // 실행전 테이블 초기화 코드
        resetData();

        // 1. Event 생성
        Event event = createEvent();
        eventRepository.save(event);

        // Team 생성
        Team homeTeam = createTeam("T1");
        Team awayTeam = createTeam("Gen.G");
        teamRepository.save(homeTeam);
        teamRepository.save(awayTeam);

        // Match 생성
        match = createMatch(event, homeTeam, awayTeam);
        matchRepository.save(match);

        // SeatGrade 생성
        VIPGrade = createSeatGrade(event, "VIP", 100000, "FF0000");
        RGrade = createSeatGrade(event, "R", 70000, "00FF00");
        SGrade = createSeatGrade(event, "S", 50000, "0000FF");
        seatGradeRepository.save(VIPGrade);
        seatGradeRepository.save(RGrade);
        seatGradeRepository.save(SGrade);

        // Section 생성
        Section sectionA = createSection(event, "A구역", 1, null);
        Section sectionB = createSection(event, "B구역", 2, null);
        Section sectionC = createSection(event, "C구역", 3, null);
        Section sectionD = createSection(event, "D구역", 4, null);
        sectionRepository.save(sectionA);
        sectionRepository.save(sectionB);
        sectionRepository.save(sectionC);
        sectionRepository.save(sectionD);

        // Seat 생성
        createSeat(sectionA);
        createSeat(sectionB);
        createSeat(sectionC);
        createSeat(sectionD);

        log.info("[SeedData] ✅ 시드 데이터 삽입 완료!");
    }

    /**
     * Event 엔티티 생성 및 반환 메소드
     */
    private Event createEvent() {
        Event event = Event.builder()
                .title("2026 LCK Spring 결승전")
                .sportType(SportType.LOL)
                .place("LOL Park")
                .thumbnailUrl("lck_thumb.png")
                .detailImageUrl("lck_detail.png")
                .description("2026 LoL 챔피언스 코리아 스프링 시즌의 대미를 장식할 결승전!")
                .startDate(LocalDate.of(2026, 4, 25))
                .endDate(LocalDate.of(2026, 4, 25))
                .matchDurationText("약 240분")
                .ageRating("12세 이용가")
                .bookingOpenAt(LocalDateTime.of(2026, 4, 10, 18, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 24, 23, 59))
                .bookingNotice("1인당 최대 2매 예매 가능합니다.")
                .maxTicketsPerUser(2)
                .cancelAvailableUntil(LocalDateTime.of(2026, 4, 24, 17, 0))
                .cancelFee(1000)
                .status(EventStatus.OPEN)
                .build();

        return event;
    }

    /**
     * Team 엔티티 생성 및 반환 메소드
     */
    private Team createTeam(String name) {
        Team team = Team.builder()
                .name(name)
                .logoImageUrl(name.toLowerCase().replace('.', ' ').strip() + "_logo.png")
                .sportType(SportType.LOL)
                .build();

        return team;
    }

    /**
     * Match 엔티티 생성 및 반환 메소드
     */
    private Match createMatch(Event event, Team homeTeam, Team awayTeam) {
        Match match = Match.builder()
                .event(event)
                .roundLabel("플레이오프 제 3 경기")
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .matchDate(LocalDate.of(2026, 4, 25))
                .startAt(LocalDateTime.of(2026, 4, 25, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 25, 21, 0))
                .status(MatchStatus.SCHEDULED)
                .build();

        return match;
    }

    /**
     * SeatGrade 엔티티 생성 및 반환 메소드
     */
    private SeatGrade createSeatGrade(Event event, String gradeCode, int price, String colorHex) {
        SeatGrade seatGrade = SeatGrade.create(
                event,
                gradeCode,
                price,
                colorHex
        );

        return seatGrade;
    }

    /**
     * Section 엔티티 생성 및 반환 메소드
     */
    private Section createSection(Event event, String name, int displayOrder, String description) {
        Section section = Section.create(
                event,
                name,
                displayOrder,
                description
        );

        return section;
    }

    /**
     * 구역별 좌석 생성 메소드 (100석 씩)
     */
    public void createSeat(Section section) {
        List<Seat> seatList = new ArrayList<>();

        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 10; c++) {
                SeatGrade seatGrade;

                switch (r) {
                    // 앞열 2줄은 VIP 등급
                    case 1, 2:
                        seatGrade = VIPGrade;
                        break;
                    // 다음 3줄은 R 등급
                    case 3, 4, 5:
                        seatGrade = RGrade;
                        break;
                    // 나머지는 S 등급
                    default:
                        seatGrade = SGrade;
                        break;
                }

                Seat seat = Seat.create(
                        match,
                        section,
                        seatGrade, // 위에서 지정한 좌석 등급
                        Character.toString((char)('A' + (r-1))), // 행: A, B, C..
                        c,  // 열: 1, 2, 3...
                        createSeatCode(seatGrade.getGradeCode(), section.getName(), Character.toString((char)('A' + (r-1))), c)
                );

                seatList.add(seat);

                log.info(seat.getSeatCode() + " 좌석 생성 완료!");
            }
        }

        seatRepository.saveAll(seatList);
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
    public void resetData() {
        /**
         * 삭제 순서 seat -> section & seatgrade -> match -> team & event
         */
        seatRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }
}
