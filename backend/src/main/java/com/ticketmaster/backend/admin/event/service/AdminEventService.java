package com.ticketmaster.backend.admin.event.service;

import com.ticketmaster.backend.admin.event.dto.request.AdminEventCreateRequest;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventDetailResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventListResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventResponse;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용 (성능 향상)
public class AdminEventService {
    private final EventRepository eventRepo;
    private final BookingRepository bookingRepo;

    /**
     * 이벤트 등록 (디폴트 status: UPCOMING)
     */
    @Transactional
    public AdminEventResponse createEvent(AdminEventCreateRequest request) {
        // 예외-1) 이벤트 타이틀 중복
        if (eventRepo.existsByTitle(request.getTitle())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EVENT_TITLE);
        }

        // 예외-2) 날짜 검증
        if (request.getEndDate().isBefore(request.getStartDate())) { // 이벤트 끝나는 날짜가 시작하는 날짜보다 앞서는 경우
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getBookingCloseAt().isBefore(request.getBookingOpenAt())) { // 예매 오픈/종료일 비교 로직도 여기에 추가
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getStartDate().isBefore(request.getBookingOpenAt().toLocalDate())) { // 예매 시작일이 이벤트 시작하는 날짜보다 뒤인 경우
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 모든 예외 통과시 DB 등록
        Event saved = eventRepo.save(request.toEntity());

        return AdminEventResponse.from(saved);
    }

    /**
     * 이벤트 목록 조회 (간단한 정보 리스트)
     */
    public Page<AdminEventListResponse> getEventList(Pageable pageable) {
        Page<Event> eventPage = eventRepo.findAll(pageable);

        return eventPage.map(AdminEventListResponse::from);
    }

    /**
     * 이벤트 상세 조회 (상세 정보 리스트)
     */
    public AdminEventDetailResponse getEventDetail(Long id) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        return AdminEventDetailResponse.from(event);
    }

    /**
     * 이벤트 수정 요청
     */
    @Transactional
    public AdminEventResponse updateEvent(Long id, AdminEventUpdateRequest request) {
        // 1. 대상 이벤트 조회
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 2. 상태 변경 방어 로직 (update 하기 전에 검사!)
        // 기존 상태가 FINISHED인데 && 요청에 상태 변경값이 들어있고 && 그게 FINISHED가 아니라면 막는다!
        if (event.getStatus() == EventStatus.FINISHED &&
                request.getStatus() != null &&
                request.getStatus() != EventStatus.FINISHED) {
            throw new BusinessException(ErrorCode.CANNOT_CHANGE_FINISHED_MATCH);
        }

        // 3. 타이틀 중복 검사 로직
        if (request.getTitle() != null && !request.getTitle().equals(event.getTitle())) {
            if (eventRepo.existsByTitle(request.getTitle())) {
                throw new BusinessException(ErrorCode.DUPLICATE_EVENT_TITLE);
            }
        }

        // 4. 엔티티 수정 (일단 들어온 값들로 덮어씌우기!)
        // 이렇게 하면 변경 안 한 필드는 기존 값, 변경한 필드는 새 값으로 섞인 완벽한 객체가 됩니다.
        event.update(request);

        // 5. 날짜 논리 검증 (request DTO가 아니라 event 엔티티 기준으로 검증!)
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (event.getBookingCloseAt().isBefore(event.getBookingOpenAt())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (event.getStartDate().isBefore(event.getBookingOpenAt().toLocalDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 6. 응답 DTO 반환
        return AdminEventResponse.from(event);
    }

    /**
     * 이벤트 삭제
     */
    @Transactional
    public void deleteEvent(Long id) {
        // 1. 방어 로직: 해당 이벤트와 연관된 매치 중, 'CANCELED(취소)' 상태가 아닌 예매 내역이 존재하는지 확인
        // (하나라도 존재한다면 삭제 불가!)
        if (bookingRepo.existsByMatch_Event_IdAndStatusNot(id, BookingStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.EVENT_IN_USE);
        }

        // 2. 이벤트 조회 및 삭제
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        event.softDelete();
    }
}
