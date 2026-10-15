import { CalendarDays } from "lucide-react";

import { formatDeadline } from "@/lib/todos";
import { cn } from "@/lib/utils";

function daysUntil(deadline: string) {
  const [year, month, day] = deadline.split("-").map(Number);
  const target = new Date(year, month - 1, day);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

/**
 * Deadline chip for a todo list. Turns amber when the deadline is within a week and red once it has
 * passed — unless every item is already done, in which case it stays quiet.
 */
export default function DeadlineBadge({
  deadline,
  allDone = false,
}: {
  deadline?: string | null;
  allDone?: boolean;
}) {
  const label = formatDeadline(deadline);
  if (!deadline || !label) {
    return null;
  }

  const days = daysUntil(deadline);
  const overdue = days < 0 && !allDone;
  const soon = days >= 0 && days <= 7 && !allDone;

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium",
        overdue
          ? "border-destructive/30 bg-destructive/10 text-destructive"
          : soon
            ? "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
            : "border-border bg-muted text-muted-foreground"
      )}
    >
      <CalendarDays className="size-3.5" />
      {label}
      {overdue && <span className="opacity-80">· overdue</span>}
    </span>
  );
}
