import { BrowserRouter, Link, Route, Routes } from "react-router-dom";

import MoviePage from "@/pages/movies/MoviePage";
import MoviesPage from "@/pages/movies/MoviesPage";

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen">
        <header className="border-b">
          <div className="mx-auto flex max-w-7xl items-center gap-6 px-6 py-4">
            <Link to="/" className="font-semibold">
              Codelab
            </Link>

            <Link to="/movies" className="text-sm">
              Movies
            </Link>
          </div>
        </header>

        <main className="mx-auto max-w-7xl px-6 py-8">
          <Routes>
            <Route path="/movies" element={<MoviesPage />} />
            <Route
              path="/movies/:movieId"
              element={<MoviePage />}
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
