package com.ticketmaster.backend.admin.match;

import com.ticketmaster.backend.admin.match.dto.request.AdminMatchCreateRequest;
import com.ticketmaster.backend.admin.match.dto.request.AdminMatchUpdateRequest;
import com.ticketmaster.backend.admin.match.dto.response.AdminMatchResponse;
import com.ticketmaster.backend.admin.match.service.AdminMatchService;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.team.repository.TeamRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminMatchServiceTest {
    @Mock
    private MatchRepository matchRepo;
    @Mock
    private EventRepository eventRepo;
    @Mock
    private TeamRepository teamRepo;

    @InjectMocks
    private AdminMatchService adminMatchService;


    //--------- 전체 매치 목록 조회 --------------------------

    // 전체 매치 조회 - 정상 케이스
    @Test
    @DisplayName("전체 매치 조회 성공 - 정상 케이스")
    void 매치_목록조회_전체() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 연관 엔티티 Mocking (DTO 변환 시 getId() 호출에 대비하여 가짜 객체 생성)
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(100L);

        Team mockHomeTeam = mock(Team.class);
        given(mockHomeTeam.getId()).willReturn(10L);

        Team mockAwayTeam = mock(Team.class);
        given(mockAwayTeam.getId()).willReturn(20L);

        // 2. 가짜 Match 엔티티 생성 및 연관 객체 주입
        Match mockMatch = Match.builder()
                .event(mockEvent)
                .roundLabel("결승전")
                .homeTeam(mockHomeTeam)
                .awayTeam(mockAwayTeam)
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 21, 0))
                .status(MatchStatus.SCHEDULED)
                .build();

        // DB에서 꺼내온 것처럼 ID 부여 (JPA의 @Id 필드 강제 주입)
        ReflectionTestUtils.setField(mockMatch, "id", 1L);

        // 3. 레포지토리가 반환할 가짜 Page 객체 생성
        Page<Match> matchPage = new PageImpl<>(List.of(mockMatch), pageable, 1);

        // 4. 레포지토리(Mock)의 동작 설정
        given(matchRepo.findAll(pageable)).willReturn(matchPage);

        // When
        // 실제 테스트 대상인 서비스 메서드 호출
        Page<AdminMatchResponse> result = adminMatchService.getMatchList(isNull(), pageable);

        // Then
        // 1. 반환된 Page 객체 검증
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        // 2. DTO 변환(Mapping)이 정확하게 이루어졌는지 검증
        AdminMatchResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEventId()).isEqualTo(100L);
        assertThat(response.getRoundLabel()).isEqualTo("결승전");
        assertThat(response.getHomeTeamId()).isEqualTo(10L);
        assertThat(response.getAwayTeamId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(MatchStatus.SCHEDULED);

        // 3. 레포지토리의 findAll 메서드가 해당 파라미터로 정확히 1번 호출되었는지 검증
        verify(matchRepo).findAll(pageable);
    }

    // 전체 매치 조회 - 이벤트별 필터링
    @Test
    @DisplayName("전체 매치 조회 성공 - 이벤트별 필터링")
    void 매치_목록조회_이벤트별() {
        // Given: 필터링할 이벤트 ID와 페이지네이션 정보
        Long filterEventId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 연관 엔티티 Mocking (DTO 변환 대비)
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(filterEventId); // 우리가 필터링할 ID를 반환하도록 세팅

        Team mockHomeTeam = mock(Team.class);
        given(mockHomeTeam.getId()).willReturn(10L);

        Team mockAwayTeam = mock(Team.class);
        given(mockAwayTeam.getId()).willReturn(20L);

        // 2. 가짜 Match 엔티티 생성
        Match mockMatch = Match.builder()
                .event(mockEvent)
                .roundLabel("4강전")
                .homeTeam(mockHomeTeam)
                .awayTeam(mockAwayTeam)
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 21, 0))
                .status(MatchStatus.SCHEDULED)
                .build();
        ReflectionTestUtils.setField(mockMatch, "id", 1L);

        // 3. 레포지토리가 반환할 가짜 Page 객체 생성
        Page<Match> matchPage = new PageImpl<>(List.of(mockMatch), pageable, 1);

        // 4. 레포지토리 동작 설정: findAll()이 아닌 findByEventId()가 호출될 것을 기대!
        given(matchRepo.findByEventId(filterEventId, pageable)).willReturn(matchPage);

        // When: 서비스 메서드에 eventId를 포함하여 호출
        Page<AdminMatchResponse> result = adminMatchService.getMatchList(filterEventId, pageable);

        // Then
        // 1. 반환된 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventId()).isEqualTo(filterEventId);

        // 2. 레포지토리의 findByEventId 메서드가 정확히 호출되었는지 확인!
        verify(matchRepo).findByEventId(filterEventId, pageable);
    }

    //--------- 매치 등록 --------------------------

    // 매치 등록 - 정상 케이스
    @Test
    @DisplayName("매치 등록 성공 - 정상 케이스")
    void 매치_등록_정상() {
        // Given
        Long eventId = 100L;
        Long homeTeamId = 10L;
        Long awayTeamId = 20L;

        // 1. 요청 파라미터 준비
        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 21, 0))
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .build();

        // 2. 조회될 가짜 엔티티 준비 및 레포지토리 동작 설정 (Event, Team)
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(eventId);
        given(eventRepo.findById(eventId)).willReturn(Optional.of(mockEvent));

        Team mockHomeTeam = mock(Team.class);
        given(mockHomeTeam.getId()).willReturn(homeTeamId);
        given(teamRepo.findById(homeTeamId)).willReturn(Optional.of(mockHomeTeam));

        Team mockAwayTeam = mock(Team.class);
        given(mockAwayTeam.getId()).willReturn(awayTeamId);
        given(teamRepo.findById(awayTeamId)).willReturn(Optional.of(mockAwayTeam));

        // 3. save() 되었을 때 반환될 가짜 Match 엔티티 준비
        Match savedMatch = Match.builder()
                .event(mockEvent)
                .roundLabel(request.getRoundLabel())
                .homeTeam(mockHomeTeam)
                .awayTeam(mockAwayTeam)
                .matchDate(request.getMatchDate())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(MatchStatus.SCHEDULED) // 기본 상태
                .build();
        ReflectionTestUtils.setField(savedMatch, "id", 1L); // DB에서 자동 생성되는 PK 강제 주입

        // 레포지토리의 save 동작 설정
        given(matchRepo.save(any(Match.class))).willReturn(savedMatch);

        // When
        // 실제 테스트 대상인 서비스 메서드 호출
        AdminMatchResponse response = adminMatchService.createMatch(eventId, request);

        // Then
        // 1. 반환된 DTO 데이터 검증
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getRoundLabel()).isEqualTo("결승전");
        assertThat(response.getHomeTeamId()).isEqualTo(homeTeamId);
        assertThat(response.getAwayTeamId()).isEqualTo(awayTeamId);
        assertThat(response.getStatus()).isEqualTo(MatchStatus.SCHEDULED);

        // 2. 레포지토리가 정확히 호출되었는지 흐름 검증
        verify(eventRepo).findById(eventId); // 이벤트 조회가 발생했는가?
        verify(teamRepo).findById(homeTeamId); // 홈팀 조회가 발생했는가?
        verify(teamRepo).findById(awayTeamId); // 원정팀 조회가 발생했는가?
        verify(matchRepo).save(any(Match.class)); // 최종적으로 저장이 발생했는가?
    }

    // 매치 등록 - 대진 미정(homeTeam/awayTeam null)으로 생성
    @Test
    @DisplayName("매치 등록 성공 - 대진 미정으로 생성")
    void 매치_등록_대진미정() {
        // Given
        Long eventId = 100L;

        // 1. 요청 파라미터 준비 (팀 ID를 null로 세팅)
        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전 (대진 미정)")
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 21, 0))
                .homeTeamId(null) // 👈 대진 미정
                .awayTeamId(null) // 👈 대진 미정
                .build();

        // 2. 조회될 가짜 엔티티 준비 (Event만 필요함)
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(eventId);
        given(eventRepo.findById(eventId)).willReturn(Optional.of(mockEvent));

        // ★ 팀 ID가 null이므로 teamRepo.findById()는 호출되지 않음! (Mocking 생략)

        // 3. save() 되었을 때 반환될 가짜 Match 엔티티 준비 (팀은 null)
        Match savedMatch = Match.builder()
                .event(mockEvent)
                .roundLabel(request.getRoundLabel())
                .homeTeam(null) // 엔티티에도 null 반영
                .awayTeam(null) // 엔티티에도 null 반영
                .matchDate(request.getMatchDate())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(MatchStatus.SCHEDULED)
                .build();
        ReflectionTestUtils.setField(savedMatch, "id", 1L);

        given(matchRepo.save(any(Match.class))).willReturn(savedMatch);

        // When
        // 실제 테스트 대상 호출
        AdminMatchResponse response = adminMatchService.createMatch(eventId, request);

        // Then
        // 1. 반환된 DTO 데이터 검증
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRoundLabel()).isEqualTo("결승전 (대진 미정)");

        // 핵심 검증: 응답 DTO에도 팀 ID가 null로 잘 들어있는가?
        assertThat(response.getHomeTeamId()).isNull();
        assertThat(response.getAwayTeamId()).isNull();

        // 2. 레포지토리 호출 흐름 검증
        verify(eventRepo).findById(eventId); // 이벤트는 조회되어야 함
        verify(matchRepo).save(any(Match.class)); // 저장은 되어야 함

        // ★ 이 테스트의 꽃! 팀 레포지토리의 findById는 '절대로(never)' 호출되지 않았어야 함!
        verify(teamRepo, never()).findById(anyLong());
    }

    // 매치 등록 - 존재하지 않는 이벤트인 경우
    @Test
    @DisplayName("매치 등록 실패 - 존재하지 않는 이벤트")
    void 매치_등록_존재하지않는이벤트() {
        // Given
        Long invalidEventId = 999L; // 존재하지 않는 임의의 ID

        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .homeTeamId(10L)
                .awayTeamId(20L)
                .build();

        // ★ 핵심: DB에서 이벤트를 찾지 못함 (빈 Optional 반환)
        given(eventRepo.findById(invalidEventId)).willReturn(Optional.empty());

        // When & Then
        // AssertJ의 assertThatThrownBy를 사용하여 예외가 발생하는지 검증
        assertThatThrownBy(() -> adminMatchService.createMatch(invalidEventId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVENT_NOT_FOUND);
        // 프로젝트의 BusinessException 구조에 따라 ErrorCode를 검증합니다.

        // 💡 흐름 검증 (매우 중요)
        verify(eventRepo).findById(invalidEventId); // 이벤트 조회는 시도했어야 함

        // 이벤트가 없어서 예외가 터졌으므로, 그 아래에 있는 팀 조회나 매치 저장은 단 한 번도 실행되지 않아야 함!
        verify(teamRepo, never()).findById(anyLong());
        verify(matchRepo, never()).save(any(Match.class));
    }

    // 매치 등록 - 존재하지 않는 팀인 경우
    @Test
    @DisplayName("매치 등록 실패 - 존재하지 않는 팀")
    void 매치_등록_존재하지않는팀() {
        // Given
        Long eventId = 100L;
        Long invalidHomeTeamId = 999L; // 존재하지 않는 가짜 팀 ID
        Long awayTeamId = 20L;

        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .homeTeamId(invalidHomeTeamId)
                .awayTeamId(awayTeamId)
                .build();

        // 1. 이벤트는 정상적으로 존재한다고 가정 (통과해야 함)
        Event mockEvent = mock(Event.class);
        given(eventRepo.findById(eventId)).willReturn(Optional.of(mockEvent));

        // 2. ★ 핵심: 홈팀 조회 시 DB에 데이터가 없음을 시뮬레이션
        given(teamRepo.findById(invalidHomeTeamId)).willReturn(Optional.empty());

        // When & Then
        // 예외가 발생하는지 검증
        assertThatThrownBy(() -> adminMatchService.createMatch(eventId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAM_NOT_FOUND);

        // 💡 호출 흐름 검증
        verify(eventRepo).findById(eventId); // 이벤트 조회는 정상적으로 실행됨
        verify(teamRepo).findById(invalidHomeTeamId); // 홈팀 조회도 실행됨 (여기서 예외 발생)

        // 예외 발생으로 로직이 중단되었으므로,
        // 아래에 있는 원정팀(AwayTeam) 조회나 매치 저장은 단 한 번도 실행되지 않아야 함!
        verify(teamRepo, never()).findById(awayTeamId);
        verify(matchRepo, never()).save(any(Match.class));
    }

    // 매치 등록 - 매치 기간이 이벤트 기간을 벗어난 경우
    @Test
    @DisplayName("매치 등록 실패 - 매치 기간이 이벤트 기간을 벗어남")
    void 매치_등록_매치기간오류() {
        // Given
        Long eventId = 100L;

        // 1. 이벤트 기간 설정 (예: 2026년 5월 1일 ~ 5월 31일)
        LocalDate eventStartDate = LocalDate.of(2026, 5, 1);
        LocalDate eventEndDate = LocalDate.of(2026, 5, 31);

        // 이벤트 Mocking: 정상적으로 조회되며, 정해진 시작/종료일을 반환함
        Event mockEvent = mock(Event.class);
        given(mockEvent.getStartDate()).willReturn(eventStartDate); // 이벤트 Mocking: 시작일(startDate)만 호출되므로 이것만 세팅합니다
        given(eventRepo.findById(eventId)).willReturn(Optional.of(mockEvent));

        // 2. 요청 파라미터 준비: 매치 날짜를 이벤트 기간 '이전'인 4월 30일로 악의적 세팅!
        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 4, 30)) // 👈 기간 벗어남!
                .startAt(LocalDateTime.of(2026, 4, 30, 17, 0))
                .homeTeamId(10L)
                .awayTeamId(20L)
                .build();

        // When & Then
        // INVALID_MATCH_DATE 예외가 정상적으로 터지는지 검증
        assertThatThrownBy(() -> adminMatchService.createMatch(eventId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MATCH_DATE);

        // 💡 호출 흐름 검증 (Fail-Fast 로직 최적화 검증)
        verify(eventRepo).findById(eventId); // 이벤트는 조회되었음

        // 날짜 검증에서 실패하여 조기 종료(Early Exit) 되었으므로,
        // 쓸데없이 DB에서 팀을 조회(teamRepo)하거나 저장(matchRepo)하는 쿼리가 발생하지 않았음을 증명!
        verify(teamRepo, never()).findById(anyLong());
        verify(matchRepo, never()).save(any(Match.class));
    }

    // 매치 등록 - 매치 시작시간이 종료시간보다 늦는 경우
    @Test
    @DisplayName("매치 등록 실패 - 매치 시작 시간이 종료 시간보다 늦음")
    void 매치_등록_시작시간오류() {
        // Given
        Long eventId = 100L;

        // 1. 이벤트 기간 설정 (정상 통과를 위해 여유 있게 세팅)
        LocalDate eventStartDate = LocalDate.of(2026, 5, 1);
        LocalDate eventEndDate = LocalDate.of(2026, 5, 31);

        // 앞선 두 관문을 무사히 통과하도록 이벤트 Mocking 세팅
        Event mockEvent = mock(Event.class);
        given(mockEvent.getStartDate()).willReturn(eventStartDate);
        given(mockEvent.getEndDate()).willReturn(eventEndDate);
        // getId()는 예외 발생 전에 호출되지 않으므로 세팅하지 않습니다. (UnnecessaryStubbingException 방지)

        given(eventRepo.findById(eventId)).willReturn(Optional.of(mockEvent));

        // 2. 요청 파라미터 준비: 날짜는 정상(5.15)이지만, 종료 시간(18:00)이 시작 시간(20:00)보다 빠름!
        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 5, 15)) // 👈 이벤트 기간(5.1~5.31) 내에 포함되어 정상 통과!
                .startAt(LocalDateTime.of(2026, 5, 15, 20, 0)) // 시작: 저녁 8시
                .endAt(LocalDateTime.of(2026, 5, 15, 18, 0))   // 종료: 저녁 6시 (오류 발생 유도)
                .homeTeamId(10L)
                .awayTeamId(20L)
                .build();

        // When & Then
        // INVALID_TIME_RANGE 예외가 정상적으로 터지는지 검증
        assertThatThrownBy(() -> adminMatchService.createMatch(eventId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TIME_RANGE);

        // 💡 호출 흐름 검증
        verify(eventRepo).findById(eventId); // 이벤트 조회는 정상적으로 수행됨

        // 시간 검증에서 실패하여 조기 종료되었으므로,
        // 그 아래에 있는 팀 조회나 저장은 단 한 번도 실행되지 않았음을 증명!
        verify(teamRepo, never()).findById(anyLong());
        verify(matchRepo, never()).save(any(Match.class));
    }

    //--------- 특정 매치 상세 조회 (수정페이지 들어갈때) --------------------------

    // 매치 상세 조회 - 정상 케이스
    @Test
    @DisplayName("매치 상세 조회 성공 - 정상 케이스")
    void 매치_상세조회_정상() {
        // Given
        Long matchId = 1L;

        // 1. DTO 변환 시 getId() 호출에 대비하여 연관 엔티티 Mocking
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(100L);

        Team mockHomeTeam = mock(Team.class);
        given(mockHomeTeam.getId()).willReturn(10L);

        Team mockAwayTeam = mock(Team.class);
        given(mockAwayTeam.getId()).willReturn(20L);

        // 2. 레포지토리에서 반환될 가짜 Match 엔티티 생성
        Match mockMatch = Match.builder()
                .event(mockEvent)
                .roundLabel("4강전")
                .homeTeam(mockHomeTeam)
                .awayTeam(mockAwayTeam)
                .matchDate(LocalDate.of(2026, 5, 10))
                .startAt(LocalDateTime.of(2026, 5, 10, 18, 0))
                .endAt(LocalDateTime.of(2026, 5, 10, 20, 0))
                .status(MatchStatus.SCHEDULED)
                .build();

        // DB에서 조회된 것처럼 ID 강제 주입
        ReflectionTestUtils.setField(mockMatch, "id", matchId);

        // 3. 레포지토리 동작 설정: findById 호출 시 mockMatch를 감싼 Optional 반환
        given(matchRepo.findById(matchId)).willReturn(Optional.of(mockMatch));

        // When
        // 실제 테스트 대상인 서비스 메서드 호출
        AdminMatchResponse response = adminMatchService.getMatchDetail(matchId);

        // Then
        // 1. 반환된 DTO 객체가 우리가 세팅한 mockMatch의 데이터를 잘 담고 있는지 검증
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(matchId);
        assertThat(response.getEventId()).isEqualTo(100L);
        assertThat(response.getRoundLabel()).isEqualTo("4강전");
        assertThat(response.getHomeTeamId()).isEqualTo(10L);
        assertThat(response.getAwayTeamId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(MatchStatus.SCHEDULED);

        // 2. 레포지토리의 findById 메서드가 파라미터(matchId)와 함께 정확히 1번 호출되었는지 검증
        verify(matchRepo).findById(matchId);
    }

    // 매치 상세 조회 - 존재하지 않는 매치
    @Test
    @DisplayName("매치 상세 조회 실패 - 존재하지 않는 매치")
    void 매치_상세조회_존재하지않는매치() {
        // Given
        Long invalidMatchId = 999L; // 존재하지 않는 임의의 매치 ID

        // ★ 핵심: DB에서 매치를 찾지 못함 (빈 Optional 반환)
        given(matchRepo.findById(invalidMatchId)).willReturn(Optional.empty());

        // When & Then
        // AssertJ를 사용하여 BusinessException과 ErrorCode 검증
        assertThatThrownBy(() -> adminMatchService.getMatchDetail(invalidMatchId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);

        // 💡 호출 흐름 검증
        // 예외가 발생하더라도 findById는 시도했어야 하므로 검증합니다.
        verify(matchRepo).findById(invalidMatchId);
    }

    //--------- 특정 매치 수정 요청 --------------------------

    // 매치 수정 - 정상 케이스
    @Test
    @DisplayName("매치 수정 성공 - 정상 케이스")
    void 매치_수정_정상() {
        // Given
        Long matchId = 1L;
        Long newHomeTeamId = 30L;
        Long newAwayTeamId = 40L;

        // 1. 이벤트 Mocking (기간 검증용)
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(100L);
        given(mockEvent.getStartDate()).willReturn(LocalDate.of(2026, 5, 1));
        given(mockEvent.getEndDate()).willReturn(LocalDate.of(2026, 5, 31));

        // 2. 기존 DB에 있던 원래 Match 엔티티 세팅 (가짜 객체)
        Match existingMatch = Match.builder()
                .event(mockEvent)
                .roundLabel("4강전") // 기존 라벨
                .matchDate(LocalDate.of(2026, 5, 10))
                .startAt(LocalDateTime.of(2026, 5, 10, 18, 0))
                .endAt(LocalDateTime.of(2026, 5, 10, 20, 0))
                .status(MatchStatus.SCHEDULED) // 기존 상태
                // homeTeam, awayTeam은 기존에 null 이었다고 가정 (대진 미정)
                .build();
        ReflectionTestUtils.setField(existingMatch, "id", matchId);

        // 3. 수정 요청(Request) DTO 세팅 (라벨, 대진팀, 상태만 부분 수정)
        AdminMatchUpdateRequest request = AdminMatchUpdateRequest.builder()
                .roundLabel("결승전 (라벨 수정)")
                .homeTeamId(newHomeTeamId)
                .awayTeamId(newAwayTeamId)
                .status(MatchStatus.LIVE)
                // matchDate, startAt, endAt은 null로 두어 수정하지 않음을 의도함
                .build();

        // 4. 새롭게 배정될 팀 Mocking
        Team mockHomeTeam = mock(Team.class);
        given(mockHomeTeam.getId()).willReturn(newHomeTeamId);

        Team mockAwayTeam = mock(Team.class);
        given(mockAwayTeam.getId()).willReturn(newAwayTeamId);

        // 5. 레포지토리 동작 설정 (Stubbing)
        given(matchRepo.findById(matchId)).willReturn(Optional.of(existingMatch));
        given(teamRepo.findById(newHomeTeamId)).willReturn(Optional.of(mockHomeTeam));
        given(teamRepo.findById(newAwayTeamId)).willReturn(Optional.of(mockAwayTeam));

        // When
        // 서비스 메서드 실행
        AdminMatchResponse response = adminMatchService.updateMatch(matchId, request);

        // Then
        // 1. 응답(Response)에 수정된 데이터가 잘 반영되었는지 확인
        assertThat(response.getId()).isEqualTo(matchId);
        assertThat(response.getRoundLabel()).isEqualTo("결승전 (라벨 수정)"); // 변경됨
        assertThat(response.getHomeTeamId()).isEqualTo(newHomeTeamId); // 변경됨
        assertThat(response.getAwayTeamId()).isEqualTo(newAwayTeamId); // 변경됨
        assertThat(response.getStatus()).isEqualTo(MatchStatus.LIVE); // 변경됨

        // 2. 값이 안 들어간 항목(null)은 기존 데이터를 그대로 유지하는지 확인
        assertThat(response.getMatchDate()).isEqualTo(LocalDate.of(2026, 5, 10)); // 유지됨
        assertThat(response.getStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 18, 0)); // 유지됨

        // 3. 레포지토리 조회 흐름 검증
        verify(matchRepo).findById(matchId);
        verify(teamRepo).findById(newHomeTeamId);
        verify(teamRepo).findById(newAwayTeamId);

        // ★ 주의: JPA 더티 체킹을 사용하므로 verify(matchRepo).save(...) 는 검증하지 않습니다!
    }

    // 매치 수정 - 대진팀 지정 (null -> Team 설정)
    @Test
    @DisplayName("매치 수정 성공 - 대진팀 지정")
    void 매치_수정_대진팀지정() {
        // Given
        Long matchId = 1L;
        Long newHomeTeamId = 10L;
        Long newAwayTeamId = 20L;

        // 1. 이벤트 Mocking (기간 검증 및 DTO 변환용)
        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(100L);
        given(mockEvent.getStartDate()).willReturn(LocalDate.of(2026, 5, 1));
        given(mockEvent.getEndDate()).willReturn(LocalDate.of(2026, 5, 31));

        // 2. 기존 DB에 있던 매치 세팅 (★ 대진 미정 상태이므로 팀은 null)
        Match existingMatch = Match.builder()
                .event(mockEvent)
                .roundLabel("결승전 (대진 미정)")
                .matchDate(LocalDate.of(2026, 5, 15))
                .startAt(LocalDateTime.of(2026, 5, 15, 18, 0))
                .endAt(LocalDateTime.of(2026, 5, 15, 20, 0))
                .status(MatchStatus.SCHEDULED)
                .homeTeam(null) // 👈 기존엔 null
                .awayTeam(null) // 👈 기존엔 null
                .build();
        ReflectionTestUtils.setField(existingMatch, "id", matchId);

        // 3. 수정 요청 DTO: 팀이 확정되어 팀 ID를 보냄
        AdminMatchUpdateRequest request = AdminMatchUpdateRequest.builder()
                .roundLabel("결승전 (대진 확정)") // 라벨도 확정으로 변경
                .homeTeamId(newHomeTeamId)     // 👈 확정된 홈팀 ID
                .awayTeamId(newAwayTeamId)     // 👈 확정된 원정팀 ID
                .build();

        // 4. 새롭게 배정될 팀 Mocking (DTO 변환 시 getId() 호출 대비)
        Team mockHomeTeam = mock(Team.class);
        given(mockHomeTeam.getId()).willReturn(newHomeTeamId);

        Team mockAwayTeam = mock(Team.class);
        given(mockAwayTeam.getId()).willReturn(newAwayTeamId);

        // 5. 레포지토리 동작 설정 (Stubbing)
        given(matchRepo.findById(matchId)).willReturn(Optional.of(existingMatch));
        given(teamRepo.findById(newHomeTeamId)).willReturn(Optional.of(mockHomeTeam));
        given(teamRepo.findById(newAwayTeamId)).willReturn(Optional.of(mockAwayTeam));

        // When
        // 서비스 메서드 실행
        AdminMatchResponse response = adminMatchService.updateMatch(matchId, request);

        // Then
        // 1. 응답에 기존에 null이었던 팀 ID가 제대로 반영되었는지 검증!
        assertThat(response.getId()).isEqualTo(matchId);
        assertThat(response.getRoundLabel()).isEqualTo("결승전 (대진 확정)");
        assertThat(response.getHomeTeamId()).isEqualTo(newHomeTeamId); // null -> 10L 로 변경됨
        assertThat(response.getAwayTeamId()).isEqualTo(newAwayTeamId); // null -> 20L 로 변경됨

        // 2. 레포지토리 호출 흐름 검증
        verify(matchRepo).findById(matchId);
        verify(teamRepo).findById(newHomeTeamId); // 홈팀 조회가 일어남
        verify(teamRepo).findById(newAwayTeamId); // 원정팀 조회가 일어남
    }

    // 매치 수정 - 존재하지 않는 매치
    @Test
    @DisplayName("매치 수정 실패 - 존재하지 않는 매치")
    void 매치_수정_존재하지않는매치() {
        // Given
        Long invalidMatchId = 999L; // 존재하지 않는 임의의 매치 ID

        // 수정 요청 파라미터 (내용은 중요하지 않으므로 임의로 세팅)
        AdminMatchUpdateRequest request = AdminMatchUpdateRequest.builder()
                .roundLabel("수정 시도")
                .build();

        // ★ 핵심: DB에서 매치를 찾지 못함 (빈 Optional 반환)
        given(matchRepo.findById(invalidMatchId)).willReturn(Optional.empty());

        // When & Then
        // MATCH_NOT_FOUND 예외가 터지는지 검증
        assertThatThrownBy(() -> adminMatchService.updateMatch(invalidMatchId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);

        // 💡 호출 흐름 검증
        verify(matchRepo).findById(invalidMatchId); // 조회를 시도했는가?

        // 예외 발생으로 조기 종료(Early Exit) 되었으므로,
        // 팀 정보 조회 등 그 아래의 로직들은 단 한 번도 실행되지 않았어야 함을 증명!
        verify(teamRepo, never()).findById(anyLong());
    }

    // 매치 수정 - 이미 삭제된 매치에 대한 수정
    @Test
    @DisplayName("매치 수정 실패 - 이미 삭제된 매치")
    void 매치_수정_삭제된매치() {
        // Given
        Long matchId = 1L;

        // 수정 요청 DTO (내용은 중요하지 않으므로 임의 세팅)
        AdminMatchUpdateRequest request = AdminMatchUpdateRequest.builder()
                .roundLabel("삭제된 매치 수정 시도")
                .build();

        // ★ 핵심: 이미 삭제된 상태(deletedAt 값이 존재)의 가짜 Match 객체 생성
        Match deletedMatch = mock(Match.class);
        // getDeletedAt() 호출 시 null이 아닌 과거의 특정 시간을 반환하도록 Mocking!
        given(deletedMatch.getDeletedAt()).willReturn(LocalDateTime.now().minusDays(1));

        // DB에서 이 삭제된 매치가 조회되도록 설정
        given(matchRepo.findById(matchId)).willReturn(Optional.of(deletedMatch));

        // When & Then
        // MATCH_ALREADY_DELETED 예외가 정상적으로 터지는지 검증
        assertThatThrownBy(() -> adminMatchService.updateMatch(matchId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_ALREADY_DELETED);

        // 💡 호출 흐름 검증
        verify(matchRepo).findById(matchId); // 조회를 시도했는가?

        // 삭제된 매치라서 조기 종료(Early Exit) 되었으므로,
        // 그 아래의 날짜 검증(getEvent 등)이나 팀 정보 조회 로직은 단 한 번도 실행되지 않았어야 함!
        verify(deletedMatch, never()).getEvent();
        verify(teamRepo, never()).findById(anyLong());
    }

    // 매치 수정 - 존재하지 않는 팀
    @Test
    @DisplayName("매치 수정 실패 - 존재하지 않는 팀")
    void 매치_수정_존재하지않는팀() {

    }

    // 매치 수정 - 시작 시간이 종료 시간보다 늦는 경우
    @Test
    @DisplayName("매치 수정 실패 - 시작 시간이 종료 시간보다 늦음")
    void 매치_수정_시작시간오류() {

    }

    // 매치 수정 - 매치 기간이 이벤트 기간을 벗어난 경우
    @Test
    @DisplayName("매치 수정 실패 - 매치 기간이 이벤트 기간을 벗어남")
    void 매치_수정_매치기간오류() {

    }

    //--------- 특정 매치 삭제 요청 --------------------------

    // 매치 삭제 - 정상 케이스
    @Test
    @DisplayName("매치 삭제 성공 - 정상 케이스")
    void 매치_삭제_정상() {

    }

    // 매치 삭제 - 존재하지 않는 매치
    @Test
    @DisplayName("매치 삭제 실패 - 존재하지 않는 매치")
    void 매치_삭제_존재하지않는매치() {

    }

    // 매치 삭제 - 이미 삭제된 매치
    @Test
    @DisplayName("매치 삭제 실패 - 이미 삭제된 매치")
    void 매치_삭제_삭제된매치() {

    }

    // 매치 삭제 - 예매 이력이 존재하는 매치
    @Test
    @DisplayName("매치 삭제 실패 - 예매 이력이 존재하는 매치")
    void 매치_삭제_예매이력존재() {

    }
}
