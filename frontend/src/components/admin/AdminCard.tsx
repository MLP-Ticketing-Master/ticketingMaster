import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

interface Props {
  title: string;
  action?: ReactNode;
  className?: string;
  headerClassName?: string;
  children: ReactNode;
}

export function AdminCard({
  title,
  action,
  className,
  headerClassName,
  children,
}: Props) {
  return (
    <Card className={cn("p-6", className)}>
      <header
        className={cn(
          "mb-6 flex items-center justify-between",
          headerClassName,
        )}
      >
        <h2 className="text-xl font-bold">{title}</h2>
        {action}
      </header>
      {children}
    </Card>
  );
}
