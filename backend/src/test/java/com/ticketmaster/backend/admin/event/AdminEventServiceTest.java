package com.ticketmaster.backend.admin.event;

import com.ticketmaster.backend.admin.event.dto.request.AdminEventCreateRequest;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventDetailResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventListResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventResponse;
import com.ticketmaster.backend.admin.event.service.AdminEventService;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminEventServiceTest {
    @Mock private EventRepository eventRepository;
    @Mock private BookingRepository bookingRepository;

    // 위에서 만든 가짜 레포지토리들을 이 서비스 객체에 주입해줍니다.
    @InjectMocks private AdminEventService adminEventService;

    @Test
    @DisplayName("이벤트 등록 성공")
    void 이벤트_등록_정상() {
        // Given: DTO 준비
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // 🌟 Given: 가짜 레포지토리 조종 (가장 중요)
        // 1. 타이틀 중복 검사: "중복 없다고 대답해!" (false)
        given(eventRepository.existsByTitle(request.getTitle())).willReturn(false);

        // 2. 저장(save) 동작: "무언가 save 하려고 하면, DB에서 방금 막 ID 1번을 부여받고 저장된 것처럼 가짜 엔티티를 뱉어내!"
        Event mockSavedEvent = Event.builder()
                .id(1L) // ID가 반드시 있어야 AdminEventResponse.from()에서 NPE가 안 터집니다.
                .title(request.getTitle())
                .status(EventStatus.UPCOMING)
                .build();
        given(eventRepository.save(any(Event.class))).willReturn(mockSavedEvent);

        // When: 실제 서비스 메서드 실행 (정상적이라면 예외 없이 응답 DTO를 반환함)
        AdminEventResponse response = adminEventService.createEvent(request);

        // Then: 결과가 내 의도대로 나왔는지 철저히 검증
        // 1. 응답값이 null이 아닌지 확인
        assertThat(response).isNotNull();

        // 2. 우리가 가짜로 주입했던 ID 1L이 응답에 잘 담겨 나왔는지 확인 (응답 DTO의 필드명에 맞게 eventId 등 수정 필요)
        assertThat(response.getEventId()).isEqualTo(1L);

        // 3. 타이틀이 원본 요청과 똑같이 저장되었는지 확인
        // assertThat(response.getTitle()).isEqualTo("2026 LCK 스프링 결승");

        // ⭐️ 추가 팁: 레포지토리의 save 메서드가 정확히 1번 호출되었는지 확인 (비즈니스 로직이 DB 저장을 빼먹지 않았는지 검증)
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("이벤트 등록 실패 - 이벤트 타이틀 중복")
    void 이벤트_등록_타이틀중복() {
        // Given: DTO 준비
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title("2026 LCK 스프링 결승") // 중복될 타이틀
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // Given: 가짜 레포지토리 동작 설정 (중요 ⭐️)
        // "누군가 DB에 '2026 LCK 스프링 결승'이 있는지 물어보면, 무조건 true(있다)라고 대답해라!"
        given(eventRepository.existsByTitle("2026 LCK 스프링 결승")).willReturn(true);

        // When & Then: 서비스를 실행했을 때, 우리가 설정한 BusinessException이 터져야 정상!
        // 비즈니스 예외는 팀 규칙인 BusinessException을 사용합니다[cite: 863].
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.createEvent(request);
        });

        // 추가 검증: 발생한 예외의 에러 코드가 기획한 대로 DUPLICATE_EVENT_TITLE 인지 확인
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EVENT_TITLE);
    }

    @Test
    @DisplayName("이벤트 등록 실패 - 이벤트 진행 날짜 오류")
    void 이벤트_등록_진행일오류() {
        // Given: DTO 준비
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 3, 26)) // 끝나는 날짜가 시작하는 날짜보다 앞섬
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // When & Then: 서비스를 실행했을 때, 우리가 설정한 BusinessException이 터져야 정상!
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.createEvent(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("이벤트 등록 실패 - 이벤트 예매일 오류")
    void 이벤트_등록_예매일오류() {
        // Given: DTO 준비
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 25, 20, 0)) // 예매 시작일이 이벤트 시작일보다 늦음
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.createEvent(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    // 이벤트 목록 조회 - 정상(삭제된거까지 전부) #TC-03
    @Test
    @DisplayName("TC-03: 이벤트 목록 조회 성공 - 정상(삭제된거까지 전부)")
    void 서비스_이벤트_목록조회_정상() {
        // Given: 페이징 정보 세팅 (예: 0번째 페이지, 10개씩 조회)
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 가짜 엔티티 준비 (하나는 정상, 하나는 삭제된 것이라 가정)
        Event activeEvent = Event.builder()
                .id(1L) // (이전에 수정하신 대로 빌더나 Reflection으로 ID 세팅)
                .title("2026 LCK 스프링")
                .status(EventStatus.OPEN)
                .build();

        Event canceledEvent = Event.builder()
                .id(2L)
                .title("취소된 오버워치 대회")
                .status(EventStatus.FINISHED)
                .build();

        // [소프트 삭제된 이벤트]
        Event deletedEvent = Event.builder()
                .id(3L)
                .title("삭제된 오버워치 대회")
                .status(EventStatus.UPCOMING) // 삭제되기 전의 상태
                .build();
        deletedEvent.softDelete();

        // 2. 가짜 엔티티들을 리스트로 묶고, Page 객체(PageImpl)로 변환합니다.
        List<Event> mockEvents = List.of(activeEvent, canceledEvent, deletedEvent);
        Page<Event> mockEventPage = new PageImpl<>(mockEvents, pageable, mockEvents.size());

        // 3. "레포지토리의 findAll(pageable)이 호출되면, 우리가 만든 가짜 페이지를 반환해라!"
        given(eventRepository.findAll(any(Pageable.class))).willReturn(mockEventPage);

        // When: 서비스 메서드 실행
        Page<AdminEventListResponse> resultPage = adminEventService.getEventList(pageable);

        // Then: 변환된 결과 검증
        // 1. 결과가 null이 아니고, 총 3개의 데이터가 들어있는지 확인
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(3);

        // 2. Event -> AdminEventListResponse DTO 변환이 잘 되었는지 내용물 확인
        AdminEventListResponse firstItem = resultPage.getContent().get(0);
        assertThat(firstItem.getEventId()).isEqualTo(1L);
        assertThat(firstItem.getTitle()).isEqualTo("2026 LCK 스프링");
        assertThat(firstItem.getStatus()).isEqualTo(EventStatus.OPEN);

        AdminEventListResponse secondItem = resultPage.getContent().get(1);
        assertThat(secondItem.getEventId()).isEqualTo(2L);
        assertThat(secondItem.getTitle()).isEqualTo("취소된 오버워치 대회");

        // 3. (추가 검증) 레포지토리의 findAll 메서드가 정확히 1번 호출되었는지 확인
        verify(eventRepository).findAll(pageable);
    }

    // 단일 이벤트 상세 조회 - 정상
    @Test
    @DisplayName("단일 이벤트 상세 조회 성공 - 정상 케이스")
    void 서비스_이벤트_단일상세조회_정상() {
        // Given: 조회할 이벤트 ID와 DB에서 꺼내올 가짜 엔티티 준비
        Long targetEventId = 1L;

        Event mockEvent = Event.builder()
                .id(targetEventId)
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .description("LCK 스프링 결승전입니다.")
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .status(EventStatus.UPCOMING)
                .build();

        // 🌟 핵심: JpaRepository의 findById는 Optional<T>를 반환합니다.
        // "누군가 1L로 조회를 요청하면, 우리가 만든 mockEvent를 Optional에 담아서 반환해라!"
        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(mockEvent));

        // When: 실제 서비스의 상세 조회 로직 실행
        AdminEventDetailResponse response = adminEventService.getEventDetail(targetEventId);

        // Then: DTO로 변환된 값들이 원본 엔티티와 일치하는지 꼼꼼하게 검증
        assertThat(response).isNotNull();
        assertThat(response.getEventId()).isEqualTo(targetEventId);
        assertThat(response.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(response.getSportType()).isEqualTo(SportType.LOL);
        assertThat(response.getPlace()).isEqualTo("LoL Park");
        assertThat(response.getDescription()).isEqualTo("LCK 스프링 결승전입니다.");
        assertThat(response.getMaxTicketsPerUser()).isEqualTo(4);
        assertThat(response.getStatus()).isEqualTo(EventStatus.UPCOMING);

        // 추가 검증: 레포지토리의 findById가 1L을 파라미터로 정확히 1번 호출되었는지 확인
        verify(eventRepository).findById(targetEventId);
    }

    // 단일 이벤트 상세 조회 - 없는 이벤트를 요청한 경우
    @Test
    @DisplayName("단일 이벤트 상세 조회 실패 - 존재하지 않는 이벤트 ID")
    void 서비스_이벤트_단일상세조회_실패_NOT_FOUND() {
        // Given: 존재하지 않는 이벤트 ID
        Long wrongEventId = 999L;

        // "DB에 999L을 물어보면, 아무것도 없다고(Optional.empty()) 대답해라!"
        given(eventRepository.findById(wrongEventId)).willReturn(Optional.empty());

        // When & Then: 서비스를 실행했을 때 EVENT_NOT_FOUND 에러가 터져야 정상!
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.getEventDetail(wrongEventId);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    // 이벤트 수정 요청 - 정상(전체 수정)
    @Test
    @DisplayName("이벤트 수정 요청 성공 - 전체 수정")
    void 서비스_이벤트_수정_전체수정() {
        // Given: 수정할 타겟 ID
        Long targetEventId = 1L;

        // 1. DB에 이미 존재하는 "수정 전" 원본 가짜 엔티티 준비
        Event existingEvent = Event.builder()
                .id(targetEventId)
                .title("기존 LCK 스프링")
                .sportType(SportType.STARCRAFT) // 일부러 엉뚱한 값
                .place("기존 장소")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .status(EventStatus.UPCOMING)
                .maxTicketsPerUser(4)
                .cancelFee(0)
                .build();

        // 2. 모든 필드를 꽉꽉 채운 "수정 요청" DTO 준비
        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("KINTEX 제1전시장")
                .thumbnailUrl("https://thumb.jpg")
                .detailImageUrl("https://detail.jpg")
                .description("장소 및 일정이 변경되었습니다.")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .matchDurationText("약 5시간")
                .ageRating("12세 이상")
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .bookingNotice("취소 수수료가 변경되었습니다.")
                .maxTicketsPerUser(2) // 4장에서 2장으로 변경
                .cancelAvailableUntil(LocalDateTime.of(2026, 4, 25, 23, 59))
                .cancelFee(2000) // 0원에서 2000원으로 변경
                .status(EventStatus.OPEN) // UPCOMING에서 OPEN으로 변경
                .build();

        // 3. "레포지토리에서 1L을 찾으면, 기존 엔티티(existingEvent)를 줘라!"
        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(existingEvent));

        // When: 서비스의 수정 로직 실행
        AdminEventResponse response = adminEventService.updateEvent(targetEventId, updateRequest);

        // Then: 수정이 잘 이루어졌는지 검증
        // 1. 응답 DTO(AdminEventResponse)에 담긴 값들이 변경된 값인지 확인
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("2026 LCK 스프링 결승");
        assertThat(response.getPlace()).isEqualTo("KINTEX 제1전시장");
        assertThat(response.getStatus()).isEqualTo(EventStatus.OPEN);

        // 2. ⭐️ 핵심: AdminEventResponse에 없는 필드들도 실제 엔티티(existingEvent) 내부에서 잘 바뀌었는지 확인
        // (자바는 객체의 주소를 참조하므로, 서비스에서 event.update()를 호출하면 existingEvent의 내용물이 바뀝니다)
        assertThat(existingEvent.getMaxTicketsPerUser()).isEqualTo(2);
        assertThat(existingEvent.getCancelFee()).isEqualTo(2000);
        assertThat(existingEvent.getDescription()).isEqualTo("장소 및 일정이 변경되었습니다.");
        assertThat(existingEvent.getAgeRating()).isEqualTo("12세 이상");

        // 3. 레포지토리의 findById가 정확히 1번 호출되었는지 확인
        verify(eventRepository).findById(targetEventId);
    }

    // 이벤트 수정 요청 - 정상(부분 수정) #TC-04
    @Test
    @DisplayName("TC-04: 이벤트 수정 요청 성공 - 부분 수정")
    void 서비스_이벤트_수정_부분수정() {
        // Given: 타겟 ID
        Long targetEventId = 1L;

        // 1. 기존 DB에 있던 꽉 찬 원본 엔티티 세팅
        Event existingEvent = Event.builder()
                .id(targetEventId)
                .title("기존 LCK 스프링")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .description("기존 상세 설명입니다.")
                .maxTicketsPerUser(4)
                .status(EventStatus.UPCOMING)
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 23, 23, 59))
                .build();

        // 2. 타이틀과 장소 딱 2개만 바꾸는 부분 수정 요청 DTO
        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .title("2026 LCK 서머 결승") // 🌟 변경 요청
                .place("KINTEX 제1전시장")   // 🌟 변경 요청
                // 나머지는 빌더에 안 넣었으니 전부 null 상태가 됨
                .build();

        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(existingEvent));

        // When: 서비스 실행
        AdminEventResponse response = adminEventService.updateEvent(targetEventId, updateRequest);

        // Then
        // 1. 변경 요청한 필드들이 잘 바뀌었는지 확인
        assertThat(response.getTitle()).isEqualTo("2026 LCK 서머 결승");
        assertThat(response.getPlace()).isEqualTo("KINTEX 제1전시장");

        // 2. ⭐️ 중요: 수정 요청을 안 한(null로 보낸) 필드들은 기존 데이터를 그대로 유지하는지 확인!
        assertThat(response.getSportType()).isEqualTo(SportType.LOL);
        assertThat(response.getStatus()).isEqualTo(EventStatus.UPCOMING);

        // 엔티티 내부 값도 확인 (DTO에 없는 description 등)
        assertThat(existingEvent.getDescription()).isEqualTo("기존 상세 설명입니다.");
        assertThat(existingEvent.getMaxTicketsPerUser()).isEqualTo(4);

        verify(eventRepository).findById(targetEventId);
    }

    // 이벤트 수정 요청 - 없는 이벤트를 요청한 경우
    @Test
    @DisplayName("이벤트 수정 요청 실패 - 없는 이벤트 요청")
    void 서비스_이벤트_수정_없는이벤트요청() {
        // Given: 존재하지 않는 이벤트 ID와 대충 만든 수정 요청 DTO
        Long wrongEventId = 999L;
        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .title("아무 타이틀") // 값이 무엇이든 상관없습니다. 어차피 DB 조회 단계에서 실패합니다.
                .build();

        // 🌟 핵심: "레포지토리에서 999L을 찾으면, 데이터가 없다고(Optional.empty()) 해라!"
        given(eventRepository.findById(wrongEventId)).willReturn(Optional.empty());

        // When & Then: 서비스를 실행했을 때 EVENT_NOT_FOUND 에러가 터져야 정상!
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.updateEvent(wrongEventId, updateRequest);
        });

        // 발생한 예외의 에러 코드가 기획한 대로 EVENT_NOT_FOUND 인지 확인
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND);

        // 추가 검증: 레포지토리의 findById가 999L 파라미터로 정확히 1번 호출되었는지 확인
        verify(eventRepository).findById(wrongEventId);
    }

    // 이벤트 수정 요청 - 이벤트 타이틀 중복
    @Test
    @DisplayName("이벤트 수정 요청 실패 - 변경하려는 타이틀이 이미 존재하는 경우")
    void 서비스_이벤트_수정_타이틀중복() {
        // Given: 수정할 타겟 ID
        Long targetEventId = 1L;

        // 1. 기존 엔티티 (원래 이름: "기존 LCK 스프링")
        Event existingEvent = Event.builder()
                .id(targetEventId)
                .title("기존 LCK 스프링")
                .status(EventStatus.UPCOMING)
                .build();

        // 2. 수정 요청 DTO (바꾸려는 이름: "2026 LCK 서머 결승")
        String newTitle = "2026 LCK 서머 결승";
        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .title(newTitle)
                .build();

        // 3. 레포지토리 조작
        // - "1L번 대회를 찾으면 기존 엔티티를 반환해라"
        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(existingEvent));
        // - "새로운 이름('2026 LCK 서머 결승')이 존재하는지 물어보면, 이미 다른 대회가 쓰고 있다고(true) 대답해라!"
        given(eventRepository.existsByTitle(newTitle)).willReturn(true);

        // When & Then: 서비스를 실행했을 때 중복 에러가 터져야 정상!
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.updateEvent(targetEventId, updateRequest);
        });

        // 발생한 예외의 에러 코드가 기획한 대로 DUPLICATE_EVENT_TITLE 인지 확인
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EVENT_TITLE);

        // 추가 검증: findById와 existsByTitle이 모두 호출되었는지 확인
        verify(eventRepository).findById(targetEventId);
        verify(eventRepository).existsByTitle(newTitle);
    }

    // 이벤트 수정 요청 - 날짜 검증
    @Test
    @DisplayName("이벤트 수정 요청 실패 - 날짜 검증")
    void 서비스_이벤트_수정_날짜검증() {
        // Given: 공통으로 사용할 타겟 ID와 정상적인 원본 엔티티
        Long targetEventId = 1L;
        Event existingEvent = Event.builder()
                .id(targetEventId)
                .title("정상 대회")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 23, 23, 59))
                .build();

        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(existingEvent));

        // ---------------------------------------------------------
        // 1. 이벤트 시작일과 종료일 검증 (종료일이 시작일보다 앞설 때)
        // ---------------------------------------------------------
        AdminEventUpdateRequest badDateRequest1 = AdminEventUpdateRequest.builder()
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 20)) // ❌ 에러: 끝나는 날짜가 4일 더 빠름
                .build();

        BusinessException ex1 = assertThrows(BusinessException.class, () -> {
            adminEventService.updateEvent(targetEventId, badDateRequest1);
        });
        assertThat(ex1.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);

        // ---------------------------------------------------------
        // 2. 이벤트 예매 시작 시간과 종료 시간 검증 (종료가 오픈보다 앞설 때)
        // ---------------------------------------------------------
        AdminEventUpdateRequest badDateRequest2 = AdminEventUpdateRequest.builder()
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 19, 20, 0)) // ❌ 에러: 예매 종료가 먼저 됨
                .build();

        BusinessException ex2 = assertThrows(BusinessException.class, () -> {
            adminEventService.updateEvent(targetEventId, badDateRequest2);
        });
        assertThat(ex2.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);

        // ---------------------------------------------------------
        // 3. 이벤트 시작일과 예매 시작 시간 검증 (예매 오픈일이 대회 시작일보다 늦을 때)
        // ---------------------------------------------------------
        AdminEventUpdateRequest badDateRequest3 = AdminEventUpdateRequest.builder()
                .startDate(LocalDate.of(2026, 4, 24))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 25, 20, 0)) // ❌ 에러: 대회 시작 후 예매 오픈
                .build();

        BusinessException ex3 = assertThrows(BusinessException.class, () -> {
            adminEventService.updateEvent(targetEventId, badDateRequest3);
        });
        assertThat(ex3.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    // 이벤트 수정 요청 - 종료된 이벤트 상태 변경 금지 #TC-08
    @Test
    @DisplayName("TC-08: 이벤트 수정 요청 실패 - 종료된 이벤트 상태 변경")
    void 서비스_이벤트_수정_종료된이벤트상태변경() {
        // Given: 이미 종료된(FINISHED) 상태의 가짜 원본 엔티티
        Long targetEventId = 1L;
        Event finishedEvent = Event.builder()
                .id(targetEventId)
                .title("2025 LCK 결승 (종료됨)")
                .status(EventStatus.FINISHED) // 🌟 핵심: 이미 FINISHED 상태
                .build();

        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(finishedEvent));

        // 변경 요청 DTO: FINISHED 상태를 다시 OPEN으로 되돌리려고 시도!
        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .status(EventStatus.OPEN)
                .build();

        // When & Then: 상태 변경 불가 예외(INVALID_STATUS_TRANSITION)가 터져야 정상
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.updateEvent(targetEventId, updateRequest);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_CHANGE_FINISHED_MATCH);
    }

    // 이벤트 소프트 삭제 - 정상 #TC-05
    @Test
    @DisplayName("TC-05: 이벤트 소프트삭제 성공 - 정상 케이스")
    void 서비스_이벤트_삭제_정상() {
        // Given: 삭제할 타겟 ID와 기존 엔티티 준비
        Long targetEventId = 1L;
        Event existingEvent = Event.builder()
                .id(targetEventId)
                .title("삭제될 LCK 대회")
                .status(EventStatus.UPCOMING)
                .build();

        // 💡 팁: 삭제 전(When 이전)에는 deletedAt이 null이어서 isDeleted()가 false인지 미리 단언해두면 더 견고한 테스트가 됩니다.
        assertThat(existingEvent.isDeleted()).isFalse();

        // "레포지토리에서 1L을 찾으면, 이 existingEvent를 줘라!"
        given(eventRepository.findById(targetEventId)).willReturn(Optional.of(existingEvent));

        // When: 삭제 서비스 로직 실행
        adminEventService.deleteEvent(targetEventId);

        // Then: 엔티티의 소프트 삭제 여부 검증
        // 1. BaseEntity의 softDelete()가 잘 호출되어 deletedAt 필드에 시간이 들어갔는지(isDeleted == true) 확인
        assertThat(existingEvent.isDeleted()).isTrue();
        assertThat(existingEvent.getDeletedAt()).isNotNull();

        // 2. 레포지토리의 findById가 정확히 1번 호출되었는지 확인
        verify(eventRepository).findById(targetEventId);
    }

    // 이벤트 소프트 삭제 - 예매 내역이 있는 match가 있는 이벤트 삭제 시도 (에러) #TC-06
    @Test
    @DisplayName("TC-06: 이벤트 소프트삭제 실패 - 예매 내역 존재")
    void 서비스_이벤트_삭제_예매내역존재() {
        // Given: 삭제 시도할 이벤트 ID
        Long targetEventId = 1L;

        // 🌟 핵심: "Booking DB에 1번 이벤트와 관련된 활성화된(CANCELED가 아닌) 예매가 있냐고 물어보면, 무조건 있다(true)고 대답해라!"
        given(bookingRepository.existsByMatch_Event_IdAndStatusNot(targetEventId, BookingStatus.CANCELED))
                .willReturn(true);

        // When & Then: 삭제 로직을 실행했을 때 EVENT_IN_USE 에러가 터져야 정상!
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminEventService.deleteEvent(targetEventId);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EVENT_IN_USE);

        // 추가 검증: 방어벽에서 막혔으므로, Event를 조회(findById)하거나 삭제하는 로직은 아예 실행조차 되지 않아야 합니다.
        // never()를 쓰면 해당 메서드가 단 한 번도 호출되지 않았음을 보장합니다.
        verify(eventRepository, never()).findById(anyLong());
    }
}
