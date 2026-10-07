package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbMovieRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbMovieRecommendationRepository
    extends JpaRepository<TmdbMovieRecommendation, Long> {}
