import { BrowserRouter, Link, NavLink, Route, Routes } from "react-router-dom";

import TodoListPage from "@/pages/todos/TodoListPage";
import TodosPage from "@/pages/todos/TodosPage";

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm transition-colors ${isActive ? "font-medium text-foreground" : "text-muted-foreground hover:text-foreground"}`;

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen">
        <header className="border-b">
          <div className="mx-auto flex max-w-7xl items-center gap-6 px-6 py-4">
            <Link to="/" className="font-semibold">
              Codelab
            </Link>

            <NavLink to="/todos" className={navLinkClass}>
              Todos
            </NavLink>
          </div>
        </header>

        <main className="mx-auto max-w-7xl px-6 py-8">
          <Routes>
            <Route path="/todos" element={<TodosPage />} />
            <Route path="/todos/:todoId" element={<TodoListPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
