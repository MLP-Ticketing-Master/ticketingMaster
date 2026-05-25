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
export async function getMatchSections(
  matchId: number,
): Promise<SeatSectionListResponse> {
  const res = await api.get<SeatSectionListResponse>(
    `/matches/${matchId}/sections`,
  );
  return res.data;
}

/**
 * 좌석 선택 2단계 — 구역 내 좌석 목록
 * GET /matches/{matchId}/sections/{sectionId}/seats
 */
export async function getSectionSeats(
  matchId: number,
  sectionId: number,
): Promise<SectionSeatList> {
  const res = await api.get<SectionSeatList>(
    `/matches/${matchId}/sections/${sectionId}/seats`,
  );
  return res.data;
}

/**
 * 좌석 일괄 점유 (USER 인증 필요)
 * POST /matches/{matchId}/seats/reserve
 */
export async function reserveSeats(
  matchId: number,
  seatIds: number[],
): Promise<SeatReserveResult> {
  const res = await api.post<SeatReserveResult>(
    `/matches/${matchId}/seats/reserve`,
    { seatIds },
  );
  return res.data;
}

/**
 * 본인 점유 좌석 해제 (USER 인증 필요)
 * DELETE /matches/{matchId}/seats/reserve
 */
export async function releaseSeats(
  matchId: number,
  seatIds: number[],
): Promise<SeatReleaseResult> {
  const res = await api.delete<SeatReleaseResult>(
    `/matches/${matchId}/seats/reserve`,
    { data: { seatIds } },
  );
  return res.data;
}
