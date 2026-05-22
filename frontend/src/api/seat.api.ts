import api from "@/lib/axios";
import type { SeatSectionListResponse } from "@/types/event";
import type { SeatLayout, Section, SeatGrade } from "@/types";

export const seatApi = {
  /**
   * GET /matches/{matchId}/sections
   * 구역 목록 + 등급별 잔여 (이벤트 상세 페이지에서 회차 선택 후 호출)
   */
  sections: (matchId: number) =>
    api
      .get<SeatSectionListResponse>(`/matches/${matchId}/sections`)
      .then((r) => r.data),

  /**
   * GET /matches/{matchId}/sections/{sectionId}/seats
   * 구역 내 개별 좌석 목록 (좌석 선택 다이얼로그용)
   */
  sectionSeats: (matchId: number, sectionId: number) =>
    api
      .get(`/matches/${matchId}/sections/${sectionId}/seats`)
      .then((r) => r.data),

  // 하위 호환 (기존 코드에서 사용)
  grades: (eventId: number) =>
    api
      .get<SeatGrade[]>(`/events/${eventId}/seat-grades`)
      .then((r) => r.data),
  layout: (matchId: number, sectionId?: number) =>
    api
      .get<SeatLayout>(`/matches/${matchId}/seats`, {
        params: sectionId ? { sectionId } : {},
      })
      .then((r) => r.data),
};
