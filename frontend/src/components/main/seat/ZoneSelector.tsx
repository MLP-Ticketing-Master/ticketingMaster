import { Button } from "@/components/ui/button";
import type { Section } from "@/types";

interface Props {
  sections: Section[];
  onSelect: (sectionId: number) => void;
}

export function ZoneSelector({ sections, onSelect }: Props) {
  const ordered = [...sections].sort((a, b) => a.sortOrder - b.sortOrder);
  return (
    <div className="flex flex-col items-center gap-12 py-10">
      <Button
        type="button"
        variant="secondary"
        className="rounded-full px-6 text-xs text-muted-foreground"
      >
        MAIN SCREEN
      </Button>

      <div className="w-full max-w-3xl rounded-xl bg-[#2D2F3E] py-6 text-center text-xl font-bold text-white">
        MAIN SCREEN
      </div>

      <div className="grid w-full max-w-3xl grid-cols-3 gap-6">
        {ordered.map((section) => (
          <button
            key={section.id}
            type="button"
            onClick={() => onSelect(section.id)}
            className="flex aspect-[3/4] items-center justify-center rounded-2xl bg-[#FF6B47] text-3xl font-bold text-white transition-transform hover:scale-105"
          >
            {sectionLabel(section.name)}
          </button>
        ))}
      </div>
    </div>
  );
}

const sectionLabel = (name: string) => name.replace(/\s?구역$/, "");
