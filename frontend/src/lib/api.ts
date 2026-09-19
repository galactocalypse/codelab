import type { GetMovieResponse, Movie } from "@/generated/src";
import { Configuration, MovieApi } from "@/generated/src";


const config = new Configuration({
  basePath: "",
});

export const movieApi = new MovieApi(config);
export async function getMovie(movieId: number) {
  return movieApi.getById1({
    id: movieId,
    includeCredits: true,
  });
}
export type { GetMovieResponse, Movie };

export function getMovies() {
  return movieApi.getAll1();
}
