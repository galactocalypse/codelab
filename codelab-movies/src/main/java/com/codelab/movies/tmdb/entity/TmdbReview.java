package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A TMDB user review. The id is a 24-character hex string, not a number.
 *
 * <p>{@code content} needs an explicit length: review bodies run to 20k+ characters, so both the
 * 255-character default and a 8k guess are too small.
 */
@Data
@Entity
@Table(name = "tmdb_reviews")
public class TmdbReview {

  @Id
  @Column(name = "review_id")
  private String reviewId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TmdbMovie movie;

  @Column(name = "author")
  private String author;

  @Column(name = "author_name")
  private String authorName;

  @Column(name = "author_username")
  private String authorUsername;

  @Column(name = "author_rating")
  private Double authorRating;

  // width 65536: review bodies run to 20k+ chars; both the 255 default and an 8k guess are too
  // small.
  @Column(name = "content", length = 65536)
  private String content;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Column(name = "url")
  private String url;
}
