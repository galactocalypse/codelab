package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbMovieCrew;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbMovieCrewRepository extends JpaRepository<TmdbMovieCrew, String> {}
