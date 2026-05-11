import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface Props {
  label: string;
  value: string;
  tone: "rose" | "blue" | "green";
}

const TONES = {
  rose: { bg: "bg-rose-50", text: "text-rose-500" },
  blue: { bg: "bg-blue-50", text: "text-blue-600" },
  green: { bg: "bg-green-50", text: "text-green-600" },
} as const;

export function AdminStatCard({ label, value, tone }: Props) {
  const c = TONES[tone];
  return (
    <Card className={cn("p-6", c.bg)}>
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className={cn("mt-2 text-3xl font-bold", c.text)}>{value}</p>
    </Card>
  );
}
