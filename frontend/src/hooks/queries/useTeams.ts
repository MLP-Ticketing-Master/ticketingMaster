import { useQuery } from "@tanstack/react-query";
import { getTeamList } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_TEAMS } from "@/lib/mock";
import type { GameType } from "@/types";

const useMock = true;

export const useTeams = (game: GameType) =>
  useQuery({
    queryKey: queryKeys.teams.list(game),
    queryFn: useMock
      ? async () =>
          game === "ALL"
            ? MOCK_TEAMS
            : MOCK_TEAMS.filter((t) => t.game === game)
      : () => getTeamList(game),
  });
