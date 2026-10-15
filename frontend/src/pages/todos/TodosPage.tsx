import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ListTodo, Plus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  createTodoList,
  listTodoLists,
  type TodoListSummary,
} from "@/lib/todos";
import DeadlineBadge from "@/pages/todos/DeadlineBadge";

export default function TodosPage() {
  const [lists, setLists] = useState<TodoListSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [deadline, setDeadline] = useState("");
  const [creating, setCreating] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    listTodoLists()
      .then(setLists)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "Unknown error"))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    if (!name.trim() || creating) {
      return;
    }
    setCreating(true);
    setError(null);
    try {
      await createTodoList({ name: name.trim(), deadline: deadline || null });
      setName("");
      setDeadline("");
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-8">
      <header className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Todos</h1>
        <p className="text-muted-foreground">
          Create a list, add items, and check them off. Each list shares one deadline.
        </p>
      </header>

      <form
        onSubmit={handleCreate}
        className="rounded-xl border bg-card p-5 shadow-sm"
      >
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="flex-1 space-y-1.5">
            <span className="text-sm font-medium">New list</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="e.g. Launch checklist"
              className="w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            />
          </label>

          <label className="space-y-1.5">
            <span className="text-sm font-medium">Deadline</span>
            <input
              type="date"
              value={deadline}
              onChange={(event) => setDeadline(event.target.value)}
              className="w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            />
          </label>

          <Button type="submit" size="lg" disabled={creating || !name.trim()}>
            <Plus />
            Create
          </Button>
        </div>
      </form>

      {error && (
        <p className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : lists.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed py-16 text-center">
          <ListTodo className="size-8 text-muted-foreground" />
          <p className="font-medium">No lists yet</p>
          <p className="text-sm text-muted-foreground">
            Create your first todo list above to get started.
          </p>
        </div>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {lists.map((list) => {
            const allDone = list.itemCount > 0 && list.doneCount === list.itemCount;
            const progress =
              list.itemCount === 0 ? 0 : Math.round((list.doneCount / list.itemCount) * 100);

            return (
              <li key={list.id}>
                <Link
                  to={`/todos/${list.id}`}
                  className="block h-full rounded-xl border bg-card p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
                >
                  <div className="flex items-start justify-between gap-3">
                    <h2
                      className={cn(
                        "font-semibold",
                        allDone && "text-muted-foreground line-through"
                      )}
                    >
                      {list.name}
                    </h2>
                    <DeadlineBadge deadline={list.deadline} allDone={allDone} />
                  </div>

                  <div className="mt-4 space-y-2">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>
                        {list.doneCount}/{list.itemCount} done
                      </span>
                      <span>{progress}%</span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-muted">
                      <div
                        className={cn(
                          "h-full rounded-full transition-all",
                          allDone ? "bg-emerald-500" : "bg-primary"
                        )}
                        style={{ width: `${progress}%` }}
                      />
                    </div>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
