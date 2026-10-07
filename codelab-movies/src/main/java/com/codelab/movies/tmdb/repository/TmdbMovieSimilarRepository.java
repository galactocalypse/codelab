package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbMovieSimilar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbMovieSimilarRepository extends JpaRepository<TmdbMovieSimilar, Long> {}
