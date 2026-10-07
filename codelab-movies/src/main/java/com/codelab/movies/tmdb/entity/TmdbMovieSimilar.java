package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * An entry from a movie's "similar" list.
 *
 * <p>{@code similarMovieId} is a bare id, not a {@code @ManyToOne}: the referenced movie often has
 * no record in the dataset, so a real foreign key would abort the load. This is a soft reference
 * and is intentionally not indexed by an FK constraint.
 */
@Data
@Entity
@Table(
    name = "tmdb_movie_similar",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_tmdb_similar",
            columnNames = {"movie_id", "similar_movie_id"}))
@IdClass(TmdbMovieSimilar.Key.class)
public class TmdbMovieSimilar {

  /**
   * Composite primary key; JPA requires a named IdClass when an entity has more than one
   * {@code @Id}.
   */
  public record Key(Long movieId, Long similarMovieId) {}

  @Id
  @Column(name = "movie_id")
  private Long movieId;

  @Id
  @Column(name = "similar_movie_id")
  private Long similarMovieId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TmdbMovie movie;

  @Column(name = "list_order", nullable = false)
  private int listOrder;
}
