package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbCountry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbCountryRepository extends JpaRepository<TmdbCountry, String> {}
