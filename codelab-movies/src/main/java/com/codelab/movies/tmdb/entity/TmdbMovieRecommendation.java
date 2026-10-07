package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * An entry from a movie's "recommendations" list. Soft reference, same reasoning as {@link
 * TmdbMovieSimilar}.
 */
@Data
@Entity
@Table(
    name = "tmdb_movie_recommendations",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_tmdb_recommendation",
            columnNames = {"movie_id", "recommended_movie_id"}))
@IdClass(TmdbMovieRecommendation.Key.class)
public class TmdbMovieRecommendation {

  /** Composite primary key; see {@link TmdbMovieSimilar.Key}. */
  public record Key(Long movieId, Long recommendedMovieId) {}

  @Id
  @Column(name = "movie_id")
  private Long movieId;

  @Id
  @Column(name = "recommended_movie_id")
  private Long recommendedMovieId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TmdbMovie movie;

  @Column(name = "media_type")
  private String mediaType;

  @Column(name = "list_order", nullable = false)
  private int listOrder;
}
