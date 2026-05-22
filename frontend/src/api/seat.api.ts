import api from "@/lib/axios";
import type {
  SeatSectionListResponse,
  SectionSeatList,
  SeatReleaseResult,
  SeatReserveResult,
} from "@/types";

/**
 * 좌석 선택 1단계 — 구역 목록 + 등급별 잔여
 * GET /matches/{matchId}/sections
 */
export const fetchMatchSections = (matchId: number) =>
  api
    .get<SeatSectionListResponse>(`/matches/${matchId}/sections`)
    .then((r) => r.data);

/**
 * 좌석 선택 2단계 — 구역 내 좌석 목록
 * GET /matches/{matchId}/sections/{sectionId}/seats
 */
export const fetchSectionSeats = (matchId: number, sectionId: number) =>
  api
    .get<SectionSeatList>(`/matches/${matchId}/sections/${sectionId}/seats`)
    .then((r) => r.data);

/**
 * 좌석 일괄 점유 (USER 인증 필요)
 * POST /matches/{matchId}/seats/reserve
 */
export const reserveSeats = (matchId: number, seatIds: number[]) =>
  api
    .post<SeatReserveResult>(`/matches/${matchId}/seats/reserve`, { seatIds })
    .then((r) => r.data);

/**
 * 본인 점유 좌석 해제 (USER 인증 필요)
 * DELETE /matches/{matchId}/seats/reserve
 */
export const releaseSeats = (matchId: number, seatIds: number[]) =>
  api
    .delete<SeatReleaseResult>(`/matches/${matchId}/seats/reserve`, {
      data: { seatIds },
    })
    .then((r) => r.data);

// 호출 형태 호환용 모음
export const seatApi = {
  sections: fetchMatchSections,
  sectionSeats: fetchSectionSeats,
  reserve: reserveSeats,
  release: releaseSeats,
};
