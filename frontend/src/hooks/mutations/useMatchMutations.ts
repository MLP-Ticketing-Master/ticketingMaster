import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createMatch, deleteMatch, updateMatch } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type {
  AdminMatchCreateRequest,
  AdminMatchUpdateRequest,
} from "@/types";

export const useCreateMatchMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      eventId,
      body,
    }: {
      eventId: number;
      body: AdminMatchCreateRequest;
    }) => createMatch(eventId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["admin", "matches"] }),
  });
};

export const useUpdateMatchMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      matchId,
      body,
    }: {
      matchId: number;
      body: AdminMatchUpdateRequest;
    }) => updateMatch(matchId, body),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ["admin", "matches"] });
      qc.invalidateQueries({
        queryKey: queryKeys.admin.matchDetail(vars.matchId),
      });
    },
  });
};

export const useDeleteMatchMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (matchId: number) => deleteMatch(matchId),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["admin", "matches"] }),
  });
};
