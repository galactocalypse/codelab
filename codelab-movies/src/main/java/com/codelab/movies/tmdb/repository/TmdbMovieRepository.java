package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbMovie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbMovieRepository extends JpaRepository<TmdbMovie, Long> {}
