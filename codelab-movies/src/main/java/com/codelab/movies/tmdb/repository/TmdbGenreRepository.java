package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbGenreRepository extends JpaRepository<TmdbGenre, Long> {}
