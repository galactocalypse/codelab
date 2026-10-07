package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbLanguageRepository extends JpaRepository<TmdbLanguage, String> {}
