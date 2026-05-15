package com.ticketmaster.backend.domain.event;

import com.ticketmaster.backend.domain.event.dto.response.EventListResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.event.service.EventService;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    //------------- 이벤트 목록 조회 ----------------

    // 정상 (기본)
    @Test
    @DisplayName("이벤트 목록 조회 성공")
    void 이벤트_목록조회_기본() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Event> fakeEvents = List.of(
                Event.builder()
                        .id(1L)
                        .title("2026 LCK 스프링 결승")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(2L)
                        .title("2026 리그오브레전드 월드 챔피언십")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(3L)
                        .title("발로란트 챔피언스 투어 코리아")
                        .sportType(SportType.VALORANT)
                        .place("VALORANT Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build()
        );

        Page<Event> fakeEventPage = new PageImpl<>(fakeEvents, pageable, fakeEvents.size());

        given(eventRepository.findAllBySportTypeAndStatusExceptDeleted(isNull(), isNull(), any(Pageable.class))).willReturn(fakeEventPage);

        Page<EventListResponse> resultPage = eventService.getEventList(null, null, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(3);

        EventListResponse firstEvent = resultPage.getContent().get(0);
        assertThat(firstEvent.getEventId()).isEqualTo(1L);
        assertThat(firstEvent.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(firstEvent.getStatus()).isEqualTo(EventStatus.OPEN);

        verify(eventRepository).findAllBySportTypeAndStatusExceptDeleted(null, null, pageable);
    }

    // 정상 (종목별 필터링)
    @Test
    @DisplayName("이벤트 목록 조회 성공 - 종목별 필터링")
    void 이벤트_목록조회_종목별() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Event> fakeEvents = List.of(
                Event.builder()
                        .id(1L)
                        .title("2026 LCK 스프링 결승")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(2L)
                        .title("2026 리그오브레전드 월드 챔피언십")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build()
        );

        Page<Event> fakeEventPage = new PageImpl<>(fakeEvents, pageable, fakeEvents.size());

        given(eventRepository.findAllBySportTypeAndStatusExceptDeleted(any(SportType.class), isNull(), any(Pageable.class))).willReturn(fakeEventPage);

        Page<EventListResponse> resultPage = eventService.getEventList(SportType.LOL, null, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);

        EventListResponse firstEvent = resultPage.getContent().get(0);
        assertThat(firstEvent.getEventId()).isEqualTo(1L);
        assertThat(firstEvent.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(firstEvent.getStatus()).isEqualTo(EventStatus.OPEN);

        verify(eventRepository).findAllBySportTypeAndStatusExceptDeleted(SportType.LOL, null, pageable);
    }

    // 정상 (종목별 + 상태별 필터링)
    @Test
    @DisplayName("이벤트 목록 조회 성공 - 종목별 + 상태별 필터링")
    void 이벤트_목록조회_종목별_상태별() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Event> fakeEvents = List.of(
                Event.builder()
                        .id(1L)
                        .title("2026 LCK 스프링 결승")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(2L)
                        .title("2026 리그오브레전드 월드 챔피언십")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build()
        );

        Page<Event> fakeEventPage = new PageImpl<>(fakeEvents, pageable, fakeEvents.size());

        given(eventRepository.findAllBySportTypeAndStatusExceptDeleted(any(SportType.class), any(EventStatus.class), any(Pageable.class))).willReturn(fakeEventPage);

        Page<EventListResponse> resultPage = eventService.getEventList(SportType.LOL, EventStatus.OPEN, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);

        EventListResponse firstEvent = resultPage.getContent().get(0);
        assertThat(firstEvent.getEventId()).isEqualTo(1L);
        assertThat(firstEvent.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(firstEvent.getStatus()).isEqualTo(EventStatus.OPEN);

        verify(eventRepository).findAllBySportTypeAndStatusExceptDeleted(SportType.LOL, EventStatus.OPEN, pageable);
    }

    // 정상 (등록순 정렬)
    @Test
    @DisplayName("이벤트 목록 조회 성공 - 등록순 정렬")
    void 이벤트_목록조회_정렬() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));

        List<Event> fakeEvents = List.of(
                Event.builder()
                        .id(1L)
                        .title("2026 LCK 스프링 결승")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(2L)
                        .title("2026 리그오브레전드 월드 챔피언십")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(3L)
                        .title("발로란트 챔피언스 투어 코리아")
                        .sportType(SportType.VALORANT)
                        .place("VALORANT Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build()
        );

        Page<Event> fakeEventPage = new PageImpl<>(fakeEvents, pageable, fakeEvents.size());

        given(eventRepository.findAllBySportTypeAndStatusExceptDeleted(isNull(), isNull(), any(Pageable.class))).willReturn(fakeEventPage);

        Page<EventListResponse> resultPage = eventService.getEventList(null, null, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(3);

        EventListResponse firstEvent = resultPage.getContent().get(0);
        assertThat(firstEvent.getEventId()).isEqualTo(1L);
        assertThat(firstEvent.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(firstEvent.getStatus()).isEqualTo(EventStatus.OPEN);

        verify(eventRepository).findAllBySportTypeAndStatusExceptDeleted(null, null, pageable);
    }

    // 정상 (필터링 결과 해당 이벤트가 존재하지 않음 -> 빈배열)
    @Test
    @DisplayName("이벤트 목록 조회 성공 - 해당 종목 이벤트 없음")
    void 이벤트_목록조회_빈배열() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Event> fakeEvents = new ArrayList<>();

        Page<Event> fakeEventPage = new PageImpl<>(fakeEvents, pageable, fakeEvents.size());

        given(eventRepository.findAllBySportTypeAndStatusExceptDeleted(any(SportType.class), isNull(), any(Pageable.class))).willReturn(fakeEventPage);

        Page<EventListResponse> resultPage = eventService.getEventList(SportType.STARCRAFT, null, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(0);

        verify(eventRepository).findAllBySportTypeAndStatusExceptDeleted(SportType.STARCRAFT, null, pageable);
    }

    // 정상 (삭제된 대회는 표시되지 않음)
    @Test
    @DisplayName("이벤트 목록 조회 성공 - 삭제된 대회는 나오면 안됨")
    void 이벤트_목록조회_삭제된대회() {
        Pageable pageable = PageRequest.of(0, 10);

        List<Event> fakeEvents = List.of(
                Event.builder()
                        .id(2L)
                        .title("2026 LCK 스프링 결승")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(3L)
                        .title("2026 리그오브레전드 월드 챔피언십")
                        .sportType(SportType.LOL)
                        .place("LOL Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build(),
                Event.builder()
                        .id(4L)
                        .title("발로란트 챔피언스 투어 코리아")
                        .sportType(SportType.VALORANT)
                        .place("VALORANT Park")
                        .thumbnailUrl("www.")
                        .startDate(LocalDate.of(2026, 3, 24))
                        .endDate(LocalDate.of(2026, 5, 11))
                        .status(EventStatus.OPEN)
                        .build()
        );

        Page<Event> fakeEventPage = new PageImpl<>(fakeEvents, pageable, fakeEvents.size());

        given(eventRepository.findAllBySportTypeAndStatusExceptDeleted(isNull(), isNull(), any(Pageable.class))).willReturn(fakeEventPage);

        Page<EventListResponse> resultPage = eventService.getEventList(null, null, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(3);

        EventListResponse firstEvent = resultPage.getContent().get(0); // 첫번째 이벤트는 삭제된 이벤트가 아니라 그 바로 뒤 이벤트여야함
        assertThat(firstEvent.getEventId()).isEqualTo(2L);
        assertThat(firstEvent.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(firstEvent.getStatus()).isEqualTo(EventStatus.OPEN);

        verify(eventRepository).findAllBySportTypeAndStatusExceptDeleted(null, null, pageable);
    }

    //------------- 이벤트 상세 조회 ----------------

    // 정상 (이벤트 상세 정보 + 좌석 등급 + 매치 정보 + 팀 정보 확인)

    // 비정상 (존재하지 않는 이벤트)

}
