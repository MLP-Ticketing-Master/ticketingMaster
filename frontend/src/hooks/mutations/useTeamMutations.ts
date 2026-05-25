import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createTeam, deleteTeam, updateTeam } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { CreateTeamRequest, UpdateTeamRequest } from "@/types";

export const useCreateTeamMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateTeamRequest) => createTeam(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.teams.all }),
  });
};

export const useUpdateTeamMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      teamId,
      body,
    }: {
      teamId: number;
      body: UpdateTeamRequest;
    }) => updateTeam(teamId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.teams.all }),
  });
};

export const useDeleteTeamMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (teamId: number) => deleteTeam(teamId),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.teams.all }),
  });
};
