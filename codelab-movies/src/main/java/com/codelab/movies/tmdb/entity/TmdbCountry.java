package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * ISO 3166-1 country, shared by {@code tmdb_movie_countries} and {@code
 * tmdb_movie_origin_countries}.
 *
 * <p>{@code name} is nullable on purpose: a country can first appear via a movie's bare {@code
 * origin_country} code and only gain its name later via {@code production_countries}.
 */
@Data
@Entity
@Table(name = "tmdb_countries")
public class TmdbCountry {

  @Id
  @Column(name = "iso_3166_1", length = 2)
  private String iso31661;

  @Column(name = "name")
  private String name;
}
