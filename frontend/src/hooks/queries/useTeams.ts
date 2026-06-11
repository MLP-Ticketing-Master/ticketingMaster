import { useQuery } from "@tanstack/react-query";
import { getAdminTeams } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { GameType } from "@/types";

export const useTeams = (game: GameType) =>
  useQuery({
    queryKey: queryKeys.teams.list(game),
    queryFn: () => getAdminTeams(game === "ALL" ? undefined : game),
  });
