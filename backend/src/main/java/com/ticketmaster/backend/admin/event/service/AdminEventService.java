package com.ticketmaster.backend.admin.event.service;

import com.ticketmaster.backend.admin.event.dto.request.AdminEventCreateRequest;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventDetailResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventListResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
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
    // private final MatchRepository matchRepo;

    /**
     * 이벤트 등록 (디폴트 status: UPCOMING)
     */
    @Transactional
    public AdminEventResponse createEvent(AdminEventCreateRequest request) {
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
     * 이벤트 수정
     */
    @Transactional
    public AdminEventResponse updateEvent(Long id, AdminEventUpdateRequest request) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        event.update(request);

        return AdminEventResponse.from(event);
    }

    /**
     * 이벤트 삭제
     */
    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 1. 연관 Match 존재 여부 확인
//        boolean isMatchExists = matchRepo.existsByEventIdAndDeletedAtIsNull(id);
//        if (isMatchExists) {
//            throw new BusinessException(ErrorCode.EVENT_IN_USE); // 추후 Custom Exception으로 교체
//        }

        // 2. 존재하지 않으면 소프트 삭제
        event.softDelete();
    }
}
