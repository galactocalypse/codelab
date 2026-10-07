package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbReviewRepository extends JpaRepository<TmdbReview, String> {}
