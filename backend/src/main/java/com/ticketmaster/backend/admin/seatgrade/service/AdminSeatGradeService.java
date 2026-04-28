package com.ticketmaster.backend.admin.seatgrade.service;

import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeCreateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeUpdateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.response.AdminSeatGradeResponse;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSeatGradeService {

    private final SeatGradeRepository seatGradeRepository;
    private final SeatRepository seatRepository;
    // private final EventRepository eventRepository;

    /**
     * 대회별 좌석 등급 목록 조회 — 가격 내림차순 (VIP가 가장 위)
     * - 와이어프레임 "좌석 등급 관리" 카드 렌더링용
     * - readOnly 트랜잭션 (클래스 레벨 적용됨)
     */
    public List<AdminSeatGradeResponse> findAllByEvent(Long eventId) {
        // TODO [EventRepository 머지 후 활성화]
        // if (!eventRepository.existsById(eventId)) {
        //     throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        // }

        return seatGradeRepository.findAllByEventIdOrderByPriceDesc(eventId)
                .stream()
                .map(AdminSeatGradeResponse::from)
                .toList();
    }

    /**
     * 좌석 등급 등록
     * 검증 순서: 대회 존재 → 같은 대회 내 등급 코드 중복
     * - 동일 코드의 soft-deleted 행이 있으면 복구(restore) + 값 갱신으로 재활용
     *   (DB unique 제약은 deleted_at 무관이라 신규 INSERT 시 충돌 → 복구 전략 채택)
     */
    @Transactional
    public AdminSeatGradeResponse create(Long eventId, AdminSeatGradeCreateRequest req) {
        // TODO [EventRepository 머지 후 활성화]
        // Event event = eventRepository.findById(eventId)
        //     .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        Optional<SeatGrade> existing =
                seatGradeRepository.findByEventIdAndGradeCodeIncludingDeleted(
                        eventId, req.getGradeCode());

        if (existing.isPresent()) {
            SeatGrade grade = existing.get();
            if (!grade.isDeleted()) {
                // 활성 상태면 진짜 중복
                throw new BusinessException(ErrorCode.DUPLICATE_GRADE_CODE);
            }
            // 삭제된 행 → 복구 + 가격/색상 갱신
            grade.restore();
            grade.update(req.getPrice(), req.getColorHex());
            return AdminSeatGradeResponse.from(grade);
        }

        // 정적 팩토리로 생성
        // 머지 후엔 SeatGrade.create(event, ...) 로 변경
        SeatGrade saved = seatGradeRepository.save(
                SeatGrade.create(/* event */ null,
                        req.getGradeCode(),
                        req.getPrice(),
                        req.getColorHex())
        );

        return AdminSeatGradeResponse.from(saved);
    }

    /** 부분 수정 — null 아닌 것만 반영, 더티 체킹으로 자동 갱신 */
    @Transactional
    public AdminSeatGradeResponse update(Long seatGradeId, AdminSeatGradeUpdateRequest req) {
        SeatGrade grade = seatGradeRepository.findById(seatGradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_GRADE_NOT_FOUND));
        grade.update(req.getPrice(), req.getColorHex());

        return AdminSeatGradeResponse.from(grade);
    }

    /**
     * 삭제 (soft delete) — 좌석에 배정된 등급은 차단 (가격이 사라지면 결제 정합성 깨짐)
     * - 실제 row 삭제 대신 deleted_at 만 채움
     * - @SQLRestriction("deleted_at IS NULL") 덕분에 이후 일반 조회에서 자동 제외됨
     */
    @Transactional
    public void delete(Long seatGradeId) {
        SeatGrade grade = seatGradeRepository.findById(seatGradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_GRADE_NOT_FOUND));
        if (seatRepository.existsBySeatGradeId(seatGradeId)) {
            throw new BusinessException(ErrorCode.SEAT_GRADE_IN_USE);
        }
        grade.softDelete();
    }
}
