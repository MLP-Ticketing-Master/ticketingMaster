import { Button } from "@/components/ui/button";
import { SPORT_FILTER_LABEL } from "@/lib/constants";
import { cn } from "@/lib/utils";
import type { SportType } from "@/types";

const FILTERS: SportType[] = ["ALL", "LOL", "VALORANT", "OVERWATCH", "TFT", "PUBG", "STARCRAFT"];

interface Props {
  value: SportType;
  onChange: (sport: SportType) => void;
}

export function GameFilter({ value, onChange }: Props) {
  return (
    <div className="flex flex-wrap gap-2">
      {FILTERS.map((s) => (
        <Button
          key={s}
          variant={value === s ? "default" : "outline"}
          size="sm"
          onClick={() => onChange(s)}
          className={cn(
            "rounded-full",
            value === s && "bg-[#316DFD] hover:bg-[#1C5EFD]",
          )}
        >
          {SPORT_FILTER_LABEL[s]}
        </Button>
      ))}
    </div>
  );
}
