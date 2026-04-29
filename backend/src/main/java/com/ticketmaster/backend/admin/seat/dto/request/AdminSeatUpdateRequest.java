package com.ticketmaster.backend.admin.seat.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PATCH — 구역/등급만 변경 (rowLabel/seatNo/seatCode 는 식별값이라 변경 X)
 * - 두 필드 모두 선택 (null 허용) → 둘 다 null 이면 변경 없이 현재 상태 반환
 * - API 명세 예시는 seatGradeId 만 보여주지만, 구역 변경도 동일 엔드포인트로 처리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSeatUpdateRequest {

    private Long sectionId;

    private Long seatGradeId;
}
