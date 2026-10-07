package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * One crew credit. Keyed by {@code credit_id} because the same person frequently holds several jobs
 * on one film (e.g. writer and director), so (movie, person) is not unique here.
 */
@Data
@Entity
@Table(name = "tmdb_movie_crew")
public class TmdbMovieCrew {

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

  @Column(name = "department", nullable = false)
  private String department;

  @Column(name = "job", nullable = false)
  private String job;
}
