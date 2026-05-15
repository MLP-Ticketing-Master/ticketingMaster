package com.ticketmaster.backend.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.event.controller.EventController;
import com.ticketmaster.backend.domain.event.dto.response.EventDetailResponse;
import com.ticketmaster.backend.domain.event.dto.response.EventListResponse;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.service.EventService;
import com.ticketmaster.backend.domain.match.dto.MatchResponse;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.seat.dto.response.SeatGradeResponse;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    /**
     * 이벤트 목록 조회 (정상)
     */
    @Test
    @DisplayName("이벤트 목록 조회 - 정상(비로그인)")
    void 이벤트_목록조회_비로그인_200() throws Exception {
        List<EventListResponse> fakeList = List.of(
                new EventListResponse(1L, "2026 LCK 스프링 결승", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(2L, "2026 리그오브레전드 월드 챔피언십", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(3L, "발로란트 챔피언스 투어 코리아", SportType.VALORANT, "VALORANT Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN)
        );
        Page<EventListResponse> fakePage = new PageImpl<>(fakeList);

        given(eventService.getEventList(isNull(), isNull(), any(Pageable.class))).willReturn(fakePage);

        mockMvc.perform(get("/events")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("2026 LCK 스프링 결승"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("이벤트 목록 조회 - 정상(로그인: 유저)")
    void 이벤트_목록조회_로그인_200() throws Exception {
        List<EventListResponse> fakeList = List.of(
                new EventListResponse(1L, "2026 LCK 스프링 결승", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(2L, "2026 리그오브레전드 월드 챔피언십", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(3L, "발로란트 챔피언스 투어 코리아", SportType.VALORANT, "VALORANT Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN)
        );
        Page<EventListResponse> fakePage = new PageImpl<>(fakeList);

        given(eventService.getEventList(isNull(), isNull(), any(Pageable.class))).willReturn(fakePage);

        mockMvc.perform(get("/events")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("2026 LCK 스프링 결승"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이벤트 목록 조회 - 정상(로그인: 관리자)")
    void 이벤트_목록조회_관리자_200() throws Exception {
        List<EventListResponse> fakeList = List.of(
                new EventListResponse(1L, "2026 LCK 스프링 결승", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(2L, "2026 리그오브레전드 월드 챔피언십", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(3L, "발로란트 챔피언스 투어 코리아", SportType.VALORANT, "VALORANT Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN)
        );
        Page<EventListResponse> fakePage = new PageImpl<>(fakeList);

        given(eventService.getEventList(isNull(), isNull(), any(Pageable.class))).willReturn(fakePage);

        mockMvc.perform(get("/events")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("2026 LCK 스프링 결승"));
    }

    /**
     * 이벤트 목록 조회 - (정상: 종목별 필터링)
     */
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("이벤트 목록 조회 - 종목별 필터링")
    void 이벤트_목록조회_종목필터링_200() throws Exception {
        List<EventListResponse> fakeList = List.of(
                new EventListResponse(1L, "2026 LCK 스프링 결승", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN),
                new EventListResponse(2L, "2026 리그오브레전드 월드 챔피언십", SportType.LOL, "LOL Park", "www", LocalDate.of(2026, 3, 24), LocalDate.of(2026, 5, 11), EventStatus.OPEN)
        );
        Page<EventListResponse> fakePage = new PageImpl<>(fakeList);

        given(eventService.getEventList(eq(SportType.LOL), isNull(), any(Pageable.class))).willReturn(fakePage);

        mockMvc.perform(get("/events?sportType=LOL")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("2026 LCK 스프링 결승"));
    }

    /**
     * 이벤트 상세 조회 (정상)
     */
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("이벤트 상세 조회 - 정상 케이스")
    void 이벤트_상세조회_정상_200() throws Exception {
        Long targetEventId = 1L;

        List<SeatGradeResponse> fakeSeatGrades = List.of(
                SeatGradeResponse.builder().seatGradeId(1L).gradeCode("VIP").price(100000).colorHex("FF0000").build(),
                SeatGradeResponse.builder().seatGradeId(2L).gradeCode("R").price(70000).colorHex("00FF00").build(),
                SeatGradeResponse.builder().seatGradeId(3L).gradeCode("C").price(50000).colorHex("0000FF").build()
        );

        Team fakeHome = Team.builder().name("T1").logoImageUrl("www.").sportType(SportType.LOL).build();
        Team fakeAway = Team.builder().name("Gen.G").logoImageUrl("www.").sportType(SportType.LOL).build();

        List<MatchResponse> fakeMatches = List.of(
                MatchResponse.builder().matchId(1L).roundLabel("플레이오프 제 1 경기")
                        .matchDate(LocalDate.of(2026, 4, 1))
                        .startAt(LocalDateTime.of(2026, 4, 1, 17, 0))
                        .homeTeam(fakeHome).awayTeam(fakeAway).status(MatchStatus.SCHEDULED).build(),
                MatchResponse.builder().matchId(2L).roundLabel("플레이오프 제 2 경기")
                        .matchDate(LocalDate.of(2026, 4, 2))
                        .startAt(LocalDateTime.of(2026, 4, 2, 17, 0))
                        .homeTeam(fakeHome).awayTeam(fakeAway).status(MatchStatus.SCHEDULED).build(),
                MatchResponse.builder().matchId(3L).roundLabel("플레이오프 제 3 경기")
                        .matchDate(LocalDate.of(2026, 4, 3))
                        .startAt(LocalDateTime.of(2026, 4, 3, 17, 0))
                        .homeTeam(fakeHome).awayTeam(fakeAway).status(MatchStatus.SCHEDULED).build()
        );

        EventDetailResponse fakeResponse = EventDetailResponse.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LOL Park")
                .detailImageUrl("https://image.ticketmaster.com/lck-2026-thumb.jpg")
                .description("2026 LCK 스프링 결승전 티켓팅입니다. 많은 관심 부탁드립니다.")
                .startDate(LocalDate.of(2026, 3, 24))
                .endDate(LocalDate.of(2026, 5, 11))
                .matchDurationText("4시간")
                .ageRating("전체관람가")
                .bookingNotice("예매는 1인당 최대 2매까지 가능하며, 취소 기한을 꼭 확인해주세요.")
                .maxTicketsPerUser(2)
                .cancelAvailableUntil(LocalDateTime.of(2026, 3, 23, 23, 59))
                .cancelFee(1000)
                .status(EventStatus.OPEN)
                .seatGrades(fakeSeatGrades)
                .matches(fakeMatches)
                .build();

        given(eventService.getEventDetail(any(Long.class))).willReturn(fakeResponse);

        mockMvc.perform(get("/events/{eventId}", targetEventId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("2026 LCK 스프링 결승"))
                .andExpect(jsonPath("$.place").value("LOL Park"));
    }

    /**
     * 이벤트 상세 조회 (비정상: 해당 이벤트 없음)
     */
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("이벤트 상세 조회 - 비정상: 해당 이벤트 없음")
    void 이벤트_상세조회_없는이벤트_404() throws Exception {
        Long targetEventId = 999L;

        given(eventService.getEventDetail(any(Long.class)))
                .willThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        mockMvc.perform(get("/events/{eventId}", targetEventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }
}
