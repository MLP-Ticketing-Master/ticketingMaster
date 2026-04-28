package com.ticketmaster.backend.admin.section.service;

import com.ticketmaster.backend.admin.section.dto.request.SectionCreateRequest;
import com.ticketmaster.backend.admin.section.dto.request.SectionUpdateRequest;
import com.ticketmaster.backend.admin.section.dto.response.SectionResponse;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSectionService {

    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;
//    private final EventRepository eventRepository;

    /**
     * 대회별 구역 목록 조회 — displayOrder 오름차순
     */
    public List<SectionResponse> findAllByEvent(Long eventId) {
        // TODO [EventRepository 머지 후 활성화]
        // if (!eventRepository.existsById(eventId)) {
        //     throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        // }

        return sectionRepository.findAllByEventIdOrderByDisplayOrderAsc(eventId)
                .stream()
                .map(SectionResponse::from)
                .toList();
    }

    /**
     * 구역 등록
     *
     * 정책:
     * - 같은 event 내에서 (name), (displayOrder) 각각 unique
     * - 이름/순서가 충돌하면 예외 → 통과 시 신규 INSERT
     */
    @Transactional
    public SectionResponse create(Long eventId, SectionCreateRequest req) {
        // TODO [EventRepository 머지 후 활성화]
        // Event event = eventRepository.findById(eventId)
        //     .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 입력 정규화: 앞뒤 공백 제거 ("VIP "와 "VIP"가 다른 이름으로 취급되지 않도록)
        String name = trim(req.getName());
        String description = trim(req.getDescription());

        // 이름 중복 체크
        if (sectionRepository.existsByEventIdAndName(eventId, name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SECTION_NAME);
        }

        // displayOrder 중복 체크
        if (sectionRepository.existsByEventIdAndDisplayOrder(eventId, req.getDisplayOrder())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SECTION_DISPLAY_ORDER);
        }

        Section saved = sectionRepository.save(
                Section.create(/* event */ null, name, req.getDisplayOrder(), description)
        );
        return SectionResponse.from(saved);
    }

    /**
     * 구역 수정
     * - 이름/순서를 변경하려는 경우에만 같은 event 내 중복 체크
     */
    @Transactional
    public SectionResponse update(Long sectionId, SectionUpdateRequest req) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        Long eventId = section.getEvent().getId();

        // 입력 정규화 — null 은 "이 필드 안 건드림" 으로 보존
        String name = trim(req.getName());
        String description = trim(req.getDescription());

        // 이름을 변경하려는 경우에만 중복 체크
        if (name != null && !name.equals(section.getName())) {
            if (sectionRepository.existsByEventIdAndName(eventId, name)) {
                throw new BusinessException(ErrorCode.DUPLICATE_SECTION_NAME);
            }
        }

        // displayOrder를 변경하려는 경우에만 중복 체크 (Integer 비교는 equals 사용)
        if (req.getDisplayOrder() != null
                && !req.getDisplayOrder().equals(section.getDisplayOrder())) {
            if (sectionRepository.existsByEventIdAndDisplayOrder(eventId, req.getDisplayOrder())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SECTION_DISPLAY_ORDER);
            }
        }

        section.update(name, req.getDisplayOrder(), description);
        return SectionResponse.from(section);
    }

    /**
     * 구역 삭제 (hard delete)
     * - 좌석에 배정된 구역은 삭제 불가
     */
    @Transactional
    public void delete(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));
        if (seatRepository.existsBySectionId(sectionId)) {
            throw new BusinessException(ErrorCode.SECTION_IN_USE);
        }
        sectionRepository.delete(section);
    }

    /** 앞뒤 공백 제거. null-safe — null이면 그대로 null 반환 */
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
