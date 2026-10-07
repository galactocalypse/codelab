package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * One cast credit. Keyed by TMDB's {@code credit_id} rather than by (movie, person) because a
 * person can legitimately play more than one character in the same film.
 */
@Data
@Entity
@Table(name = "tmdb_movie_cast")
public class TmdbMovieCast {

  @Id
  @Column(name = "credit_id")
  private String creditId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TmdbMovie movie;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  private TmdbPerson person;

  // width 1024: character names re-measured at 560 chars on the full corpus — exceeds the 255
  // default.
  @Column(name = "character", length = 1024)
  private String character;

  @Column(name = "cast_order", nullable = false)
  private int castOrder;
}
