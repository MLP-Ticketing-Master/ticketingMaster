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
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // reset 은 별도 빈에 위임 — REQUIRES_NEW 트랜잭션으로 즉시 commit (스케줄러 데드락 회피)
    private final DataResetService dataResetService;

    @Value("${seed.admin.email}")
    private String adminEmail;

    @Value("${seed.admin.password}")
    private String adminRawPassword;

    @Value("${seed.admin.nickname}")
    private String adminNickname;

    /** 모든 이벤트 공통 예매 안내 — 실제 BookingService 환불 정책과 일치 */
    private static final String BOOKING_NOTICE = """
            1인당 최대 2매까지 예매 가능합니다.
            공연 3일 전까지 무료 환불
            공연 1일 ~ 3일 전: 결제 금액의 10% 수수료
            공연 24시간 이내 취소 불가
            """;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        // 실행 전 테이블 초기화 — 별도 트랜잭션(REQUIRES_NEW)으로 즉시 commit
        dataResetService.reset();

        // 관리자 계정 시드 — 가입한 USER 데이터 보존을 위해 reset 대상에서 user 제외, 이메일 중복 체크로 멱등 처리
        seedAdminUser();

        // 날짜 기준점
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 1. Team 생성 (이벤트 공통)
        Team homeTeam = teamRepository.save(createTeam("T1"));
        Team awayTeam = teamRepository.save(createTeam("Gen.G"));

        // 2. 이벤트 6개 정의 (이벤트 기간은 모두 매치 4가지 상태가 들어갈 만큼 넉넉하게)
        List<EventSpec> specs = List.of(
                new EventSpec(
                        "2026 LCK Spring 결승전", SportType.LOL, "LoL Park",
                        "2026 LoL 챔피언스 코리아 스프링 시즌의 대미를 장식할 결승전!",
                        "lck_thumb.png", "lck_detail.png",
                        today.minusDays(30), today.plusDays(60)
                ),
                new EventSpec(
                        "LOL 챔피언스 코리아 2026 스프링 결승", SportType.LOL, "LoL Park",
                        "2026 LCK 정규 시즌 결승 무대!",
                        "lol_champions_thumb.png", "lol_champions_detail.png",
                        today.minusDays(28), today.plusDays(58)
                ),
                new EventSpec(
                        "발로란트 챔피언스 투어 코리아", SportType.VALORANT, "코엑스 컨벤션홀",
                        "전국 최강 발로란트 팀이 모인 챔피언스 투어!",
                        "valorant_thumb.png", "valorant_detail.png",
                        today.minusDays(25), today.plusDays(55)
                ),
                new EventSpec(
                        "오버워치 리그 서울 다이너스티", SportType.OVERWATCH, "e스타디움",
                        "서울 다이너스티 홈경기 직관 기회!",
                        "overwatch_thumb.png", "overwatch_detail.png",
                        today.minusDays(22), today.plusDays(52)
                ),
                new EventSpec(
                        "롤토체스 챔피언스 코리아", SportType.TFT, "서울 e스포츠 경기장",
                        "전략적 팀 전투 챔피언스 코리아 본선!",
                        "tft_thumb.png", "tft_detail.png",
                        today.minusDays(20), today.plusDays(50)
                ),
                new EventSpec(
                        "배틀그라운드 프로리그 시즌3", SportType.PUBG, "부산 벡스코",
                        "배틀그라운드 프로리그 시즌3 결승전!",
                        "pubg_thumb.png", "pubg_detail.png",
                        today.minusDays(18), today.plusDays(48)
                )
        );

        // 3. 이벤트별 풀 시드 구성
        for (EventSpec spec : specs) {
            seedEvent(spec, homeTeam, awayTeam, today, now);
        }

        log.info("[SeedData] ✅ 시드 데이터 삽입 완료!");
    }

    /**
     * 관리자 계정 시드 — 이미 존재하면 skip (멱등)
     */
    private void seedAdminUser() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[SeedData] 관리자 계정 이미 존재 — skip ({})", adminEmail);
            return;
        }
        User admin = User.createAdmin(adminEmail, passwordEncoder.encode(adminRawPassword), adminNickname);
        userRepository.save(admin);
        log.info("[SeedData] ✅ 관리자 계정 시드 완료 ({})", adminEmail);
    }

    /**
     * 단일 이벤트의 매치/구역/등급/좌석을 한 번에 시드
     */
    private void seedEvent(EventSpec spec, Team home, Team away, LocalDate today, LocalDateTime now) {
        // Event
        Event event = eventRepository.save(createEvent(spec));

        // SeatGrade 3개 (이벤트 단위)
        SeatGrade vipGrade = createSeatGrade(event, "VIP", 100000, "ef4444");
        SeatGrade rGrade   = createSeatGrade(event, "R", 70000, "10b981");
        SeatGrade sGrade   = createSeatGrade(event, "S", 50000, "3b82f6");
        seatGradeRepository.saveAll(List.of(vipGrade, rGrade, sGrade));

        // Section 4개 (이벤트 단위 — 매치 공유)
        Section sectionA = createSection(event, "A구역", 1, null);
        Section sectionB = createSection(event, "B구역", 2, null);
        Section sectionC = createSection(event, "C구역", 3, null);
        Section sectionD = createSection(event, "D구역", 4, null);
        List<Section> sections = List.of(sectionA, sectionB, sectionC, sectionD);
        sectionRepository.saveAll(sections);

        // Match 4개 (4가지 상태 시나리오)
        List<Match> matches = createMatchesForEvent(event, home, away, today, now);
        matchRepository.saveAll(matches);

        // Seat (매치 × 구역)
        for (Match match : matches) {
            for (Section section : sections) {
                createSeat(match, section, vipGrade, rGrade, sGrade);
            }
        }

        log.info("[SeedData] ✅ '{}' 시드 완료", spec.title());
    }

    /**
     * Event 엔티티 생성 및 반환 메소드
     */
    private Event createEvent(EventSpec spec) {
        return Event.builder()
                .title(spec.title())
                .sportType(spec.sportType())
                .place(spec.place())
                .thumbnailUrl(spec.thumbnailUrl())
                .detailImageUrl(spec.detailImageUrl())
                .description(spec.description())
                .startDate(spec.startDate())
                .endDate(spec.endDate())
                .matchDurationText("약 240분")
                .ageRating("12세 이용가")
                .bookingNotice(BOOKING_NOTICE)
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
     * Match 4개 생성 — 예매 시작 전 / 예매 진행 중 / 예매 마감 경기 전 / 경기 종료
     */
    private List<Match> createMatchesForEvent(Event event, Team home, Team away,
                                              LocalDate today, LocalDateTime now) {
        // Case 1: 예매 시작 전 (예매 오픈: +7일, 경기: +21일)
        Match m1 = createMatch(event, home, away,
                "4경기",
                today.plusDays(21),
                now.plusDays(21).withHour(17).withMinute(0),
                now.plusDays(21).withHour(21).withMinute(0),
                now.plusDays(7),
                now.plusDays(20).withHour(23).withMinute(59),
                now.plusDays(20).withHour(17).withMinute(0)
        );

        // Case 2: 예매 진행 중 (예매 오픈: -1일, 경기: +7일)
        Match m2 = createMatch(event, home, away,
                "3경기",
                today.plusDays(7),
                now.plusDays(7).withHour(17).withMinute(0),
                now.plusDays(7).withHour(21).withMinute(0),
                now.minusDays(1),
                now.plusDays(6).withHour(23).withMinute(59),
                now.plusDays(6).withHour(17).withMinute(0)
        );

        // Case 3: 예매 마감, 경기 전 (예매 오픈: -14일, 예매 종료: -1일, 경기: +1일)
        Match m3 = createMatch(event, home, away,
                "2경기",
                today.plusDays(1),
                now.plusDays(1).withHour(17).withMinute(0),
                now.plusDays(1).withHour(21).withMinute(0),
                now.minusDays(14),
                now.minusDays(1).withHour(23).withMinute(59),
                now.minusDays(1).withHour(17).withMinute(0)
        );

        // Case 4: 경기 종료 (예매 오픈: -21일, 경기: -7일)
        Match m4 = createMatch(event, home, away,
                "1경기",
                today.minusDays(7),
                now.minusDays(7).withHour(17).withMinute(0),
                now.minusDays(7).withHour(21).withMinute(0),
                now.minusDays(21),
                now.minusDays(8).withHour(23).withMinute(59),
                now.minusDays(8).withHour(17).withMinute(0)
        );

        return List.of(m1, m2, m3, m4);
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
     * 구역별 좌석 생성 메소드 (100석 씩) — 등급은 매개변수로 받아 이벤트별 격리
     */
    public void createSeat(Match match, Section section,
                           SeatGrade vipGrade, SeatGrade rGrade, SeatGrade sGrade) {
        List<Seat> seatList = new ArrayList<>();

        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 10; c++) {
                SeatGrade seatGrade = switch (r) {
                    case 1, 2    -> vipGrade;
                    case 3, 4, 5 -> rGrade;
                    default      -> sGrade;
                };

                Seat seat = Seat.create(
                        match,
                        section,
                        seatGrade,
                        Character.toString((char) ('A' + (r - 1))),
                        c,
                        createSeatCode(seatGrade.getGradeCode(),
                                Character.toString((char) ('A' + (r - 1))), c)
                );
                seatList.add(seat);
            }
        }

        seatRepository.saveAll(seatList);
        log.info("[SeedData] {} - {} 좌석 {}개 생성 완료", match.getRoundLabel(), section.getName(), seatList.size());
    }

    /**
     * 좌석 코드 생성기 ("등급-행-번호" ex: "VIP-A-15")
     */
    private String createSeatCode(String seatGrade, String rowLabel, Integer seatNo) {
        return seatGrade + "-" + rowLabel + "-" + seatNo;
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

    /**
     * 이벤트별 시드 정보 묶음 — title/sportType/place/description/이미지 URL/기간
     */
    private record EventSpec(
            String title,
            SportType sportType,
            String place,
            String description,
            String thumbnailUrl,
            String detailImageUrl,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
