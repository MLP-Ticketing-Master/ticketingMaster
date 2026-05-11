import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

interface Props {
  title: string;
  action?: ReactNode;
  className?: string;
  children: ReactNode;
}

export function AdminCard({ title, action, className, children }: Props) {
  return (
    <Card className={cn("p-6", className)}>
      <header className="mb-6 flex items-center justify-between">
        <h2 className="text-xl font-bold">{title}</h2>
        {action}
      </header>
      {children}
    </Card>
  );
}
