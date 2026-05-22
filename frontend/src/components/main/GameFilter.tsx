import { Button } from "@/components/ui/button";
import { SPORT_FILTER_LABEL } from "@/lib/constants";
import { cn } from "@/lib/utils";
import type { SportType } from "@/types";

const FILTERS: SportType[] = [
  "ALL",
  "LOL",
  "VALORANT",
  "OVERWATCH",
  "TFT",
  "PUBG",
  "SC2",
];

interface Props {
  value: SportType;
  onChange: (sport: SportType) => void;
}

export function GameFilter({ value, onChange }: Props) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {FILTERS.map((g) => (
        <Button
          key={s}
          variant={value === s ? "default" : "outline"}
          size="sm"
          onClick={() => onChange(s)}
          className={cn(
            "shrink-0 rounded-full",
            value === g && "bg-[#316DFD] hover:bg-[#1C5EFD]",
          )}
        >
          {SPORT_FILTER_LABEL[s]}
        </Button>
      ))}
    </div>
  );
}
