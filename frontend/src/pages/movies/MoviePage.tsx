import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import { getMovie, type GetMovieResponse } from "@/lib/api";


export default function MoviePage() {
  const { movieId } = useParams<{ movieId: string }>();

  const [data, setData] = useState<GetMovieResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!movieId) {
      setError("Movie ID is missing");
      setLoading(false);
      return;
    }

    getMovie(Number(movieId))
      .then(setData)
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unknown error");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [movieId]);

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div className="text-destructive">{error}</div>;
  }

  if (!data) {
    return <div>Movie not found.</div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold">{data.movie.name}</h1>
        <p className="text-muted-foreground">Movie #{data.movie.id}</p>
      </div>

      <section>
        <h2 className="mb-4 text-xl font-semibold">Credits</h2>

        <div className="overflow-hidden rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left">Person</th>
                <th className="px-4 py-3 text-left">Role</th>
              </tr>
            </thead>

            <tbody>
              {data.movieCredits.map((credit) => (
                <tr key={credit.id} className="border-t">
                  <td className="px-4 py-3">
                    {credit.person.name}
                  </td>
                  <td className="px-4 py-3">
                    {credit.role.name}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}