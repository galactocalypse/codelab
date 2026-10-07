package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbMovieCast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbMovieCastRepository extends JpaRepository<TmdbMovieCast, String> {}
