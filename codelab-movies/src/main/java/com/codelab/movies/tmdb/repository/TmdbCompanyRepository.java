package com.codelab.movies.tmdb.repository;

import com.codelab.movies.tmdb.entity.TmdbCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmdbCompanyRepository extends JpaRepository<TmdbCompany, Long> {}
