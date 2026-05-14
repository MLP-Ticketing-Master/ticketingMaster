import { Button } from "@/components/ui/button";
import { GAME_FILTER_LABEL } from "@/lib/constants";
import { cn } from "@/lib/utils";
import type { GameType } from "@/types";

const FILTERS: GameType[] = ["ALL", "LOL", "VALORANT", "OVERWATCH"];

interface Props {
  value: GameType;
  onChange: (game: GameType) => void;
}

export function GameFilter({ value, onChange }: Props) {
  return (
    <div className="flex gap-2">
      {FILTERS.map((g) => (
        <Button
          key={g}
          variant={value === g ? "default" : "outline"}
          size="sm"
          onClick={() => onChange(g)}
          className={cn(
            "rounded-full",
            value === g && "bg-[#316DFD] hover:bg-[#1C5EFD]",
          )}
        >
          {GAME_FILTER_LABEL[g]}
        </Button>
      ))}
    </div>
  );
}
