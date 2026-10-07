package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbCollection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbCollectionRepository extends JpaRepository<TmdbCollection, Long> {}
