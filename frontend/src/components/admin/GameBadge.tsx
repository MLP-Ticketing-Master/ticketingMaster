import { Badge } from "@/components/ui/badge";
import type { GameType } from "@/types";

const COLORS: Record<Exclude<GameType, "ALL">, string> = {
  LOL: "bg-rose-100 text-rose-600",
  VALORANT: "bg-orange-100 text-orange-600",
  OVERWATCH: "bg-amber-100 text-amber-700",
  TFT: "bg-purple-100 text-purple-600",
};

export function GameBadge({ game }: { game: GameType }) {
  if (game === "ALL") return null;
  return (
    <Badge variant="secondary" className={COLORS[game]}>
      {game}
    </Badge>
  );
}
