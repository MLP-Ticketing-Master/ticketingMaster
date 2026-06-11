import { Card } from "@/components/ui/card";

interface Props {
  value: string | number;
  label: string;
}

export function StatCard({ value, label }: Props) {
  return (
    <Card className="bg-blue-50 p-6 text-center">
      <p className="text-3xl font-bold text-[#054EFD]">{value}</p>
      <p className="mt-2 text-sm text-muted-foreground">{label}</p>
    </Card>
  );
}
