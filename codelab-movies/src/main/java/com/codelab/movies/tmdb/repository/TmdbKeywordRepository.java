package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbKeywordRepository extends JpaRepository<TmdbKeyword, Long> {}
