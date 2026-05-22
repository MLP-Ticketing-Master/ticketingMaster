import { useMutation, useQueryClient } from "@tanstack/react-query";
import { releaseSeats, reserveSeats } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

/**
 * 좌석 일괄 점유
 * POST /matches/{matchId}/seats/reserve
 */
export const useReserveSeatsMutation = (matchId: number) => {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: (seatIds: number[]) => reserveSeats(matchId, seatIds),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.matches.sections(matchId) });
      qc.invalidateQueries({
        queryKey: ["matches", "sections", matchId, "seats"],
      });
    },
  });
};

/**
 * 본인 점유 좌석 해제
 * DELETE /matches/{matchId}/seats/reserve
 */
export const useReleaseSeatsMutation = (matchId: number) => {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: (seatIds: number[]) => releaseSeats(matchId, seatIds),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.matches.sections(matchId) });
      qc.invalidateQueries({
        queryKey: ["matches", "sections", matchId, "seats"],
      });
    },
  });
};
