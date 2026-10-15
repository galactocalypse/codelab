const BASE = "/api/todos";

export interface TodoItem {
  id: number;
  description: string;
  notes?: string | null;
  done: boolean;
  todoListId?: number | null;
}

export interface TodoList {
  id: number;
  name: string;
  /** ISO date (yyyy-MM-dd) shared by every item in the list. */
  deadline?: string | null;
  items: TodoItem[];
}

export interface TodoListSummary {
  id: number;
  name: string;
  deadline?: string | null;
  itemCount: number;
  doneCount: number;
}

export interface CreateTodoListRequest {
  name: string;
  deadline?: string | null;
}

export interface AddTodoItemRequest {
  description: string;
  notes?: string | null;
}

export interface UpdateTodoItemRequest {
  description?: string | null;
  notes?: string | null;
  done?: boolean | null;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (typeof body?.message === "string") {
        message = body.message;
      }
    } catch {
      // response had no JSON body; keep the generic message
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function listTodoLists() {
  return request<TodoListSummary[]>("");
}

export function createTodoList(body: CreateTodoListRequest) {
  return request<{ id: number; name: string }>("", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function getTodoList(id: number) {
  return request<TodoList>(`/${id}`);
}

export function addTodoItem(listId: number, body: AddTodoItemRequest) {
  return request<TodoList>(`/${listId}/items`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateTodoItem(listId: number, itemId: number, body: UpdateTodoItemRequest) {
  return request<TodoList>(`/${listId}/items/${itemId}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

/** Marks an item done or undone. */
export function setTodoItemDone(listId: number, itemId: number, done: boolean) {
  const path = `/${listId}/items/${itemId}/${done ? "done" : "undone"}`;
  return request<TodoList>(path, { method: "PATCH" });
}

/** Formats an ISO date for display, or null when no deadline is set. */
export function formatDeadline(deadline?: string | null) {
  if (!deadline) {
    return null;
  }
  const [year, month, day] = deadline.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}
