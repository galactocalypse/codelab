import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Check, Pencil, Plus, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  addTodoItem,
  getTodoList,
  setTodoItemDone,
  updateTodoItem,
  type TodoItem,
  type TodoList,
} from "@/lib/todos";
import DeadlineBadge from "@/pages/todos/DeadlineBadge";

export default function TodoListPage() {
  const { todoId } = useParams<{ todoId: string }>();
  const listId = Number(todoId);

  const [list, setList] = useState<TodoList | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [description, setDescription] = useState("");
  const [notes, setNotes] = useState("");
  const [adding, setAdding] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDescription, setEditDescription] = useState("");
  const [editNotes, setEditNotes] = useState("");
  const [saving, setSaving] = useState(false);

  const refresh = useCallback(() => {
    if (!Number.isFinite(listId)) {
      setError("Invalid list id");
      setLoading(false);
      return;
    }
    setLoading(true);
    getTodoList(listId)
      .then(setList)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "Unknown error"))
      .finally(() => setLoading(false));
  }, [listId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function run(action: () => Promise<TodoList>) {
    setError(null);
    try {
      setList(await action());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    }
  }

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    if (!description.trim() || adding) {
      return;
    }
    setAdding(true);
    await run(() =>
      addTodoItem(listId, { description: description.trim(), notes: notes.trim() || null })
    );
    setDescription("");
    setNotes("");
    setAdding(false);
  }

  function startEditing(item: TodoItem) {
    setEditingId(item.id);
    setEditDescription(item.description);
    setEditNotes(item.notes ?? "");
  }

  async function saveEdit(itemId: number) {
    setSaving(true);
    await run(() =>
      updateTodoItem(listId, itemId, {
        description: editDescription.trim(),
        notes: editNotes.trim() || null,
      })
    );
    setSaving(false);
    setEditingId(null);
  }

  if (loading) {
    return <p className="text-sm text-muted-foreground">Loading…</p>;
  }

  if (!list) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Link to="/todos" className="inline-flex items-center gap-1.5 text-sm hover:underline">
          <ArrowLeft className="size-4" />
          All lists
        </Link>
        <p className="text-destructive">{error ?? "List not found"}</p>
      </div>
    );
  }

  const doneCount = list.items.filter((item) => item.done).length;
  const allDone = list.items.length > 0 && doneCount === list.items.length;
  const progress = list.items.length === 0 ? 0 : Math.round((doneCount / list.items.length) * 100);
  const ordered = [...list.items].sort((a, b) => Number(a.done) - Number(b.done) || a.id - b.id);

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <div className="space-y-4">
        <Link to="/todos" className="inline-flex items-center gap-1.5 text-sm hover:underline">
          <ArrowLeft className="size-4" />
          All lists
        </Link>

        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="space-y-1">
            <h1
              className={cn(
                "text-3xl font-bold tracking-tight",
                allDone && "text-muted-foreground line-through"
              )}
            >
              {list.name}
            </h1>
            <p className="text-sm text-muted-foreground">
              {doneCount}/{list.items.length} done
            </p>
          </div>
          <DeadlineBadge deadline={list.deadline} allDone={allDone} />
        </div>

        <div className="h-1.5 overflow-hidden rounded-full bg-muted">
          <div
            className={cn("h-full rounded-full transition-all", allDone ? "bg-emerald-500" : "bg-primary")}
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {error && (
        <p className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      <form onSubmit={handleAdd} className="space-y-3 rounded-xl border bg-card p-5 shadow-sm">
        <input
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Add an item…"
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        <textarea
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          placeholder="Notes (optional)"
          rows={2}
          className="w-full resize-y rounded-lg border bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        <div className="flex justify-end">
          <Button type="submit" size="lg" disabled={adding || !description.trim()}>
            <Plus />
            Add item
          </Button>
        </div>
      </form>

      {list.items.length === 0 ? (
        <div className="rounded-xl border border-dashed py-12 text-center">
          <p className="font-medium">Nothing here yet</p>
          <p className="text-sm text-muted-foreground">Add your first item above.</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {ordered.map((item) => {
            const editing = editingId === item.id;
            return (
              <li
                key={item.id}
                className="flex gap-3 rounded-xl border bg-card p-4 shadow-sm"
              >
                <button
                  type="button"
                  aria-pressed={item.done}
                  aria-label={item.done ? "Mark as not done" : "Mark as done"}
                  onClick={() => run(() => setTodoItemDone(listId, item.id, !item.done))}
                  className={cn(
                    "mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full border transition-colors",
                    item.done
                      ? "border-emerald-500 bg-emerald-500 text-white"
                      : "border-muted-foreground/40 hover:border-primary"
                  )}
                >
                  {item.done && <Check className="size-3" strokeWidth={3} />}
                </button>

                {editing ? (
                  <div className="flex-1 space-y-2">
                    <input
                      value={editDescription}
                      onChange={(event) => setEditDescription(event.target.value)}
                      className="w-full rounded-lg border bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                    />
                    <textarea
                      value={editNotes}
                      onChange={(event) => setEditNotes(event.target.value)}
                      placeholder="Notes"
                      rows={2}
                      className="w-full resize-y rounded-lg border bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                    />
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        onClick={() => saveEdit(item.id)}
                        disabled={saving || !editDescription.trim()}
                      >
                        Save
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => setEditingId(null)}>
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="min-w-0 flex-1">
                    <p
                      className={cn(
                        "font-medium break-words",
                        item.done && "text-muted-foreground line-through"
                      )}
                    >
                      {item.description}
                    </p>
                    {item.notes && (
                      <p className="mt-1 text-sm whitespace-pre-wrap text-muted-foreground">
                        {item.notes}
                      </p>
                    )}
                  </div>
                )}

                {!editing && (
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    aria-label="Edit item"
                    onClick={() => startEditing(item)}
                  >
                    <Pencil />
                  </Button>
                )}

                {editing && (
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    aria-label="Discard edits"
                    onClick={() => setEditingId(null)}
                  >
                    <X />
                  </Button>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
