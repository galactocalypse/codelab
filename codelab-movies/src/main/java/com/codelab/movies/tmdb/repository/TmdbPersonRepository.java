package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbPersonRepository extends JpaRepository<TmdbPerson, Long> {}
