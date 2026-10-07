package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Core TMDB movie record.
 *
 * <p>The {@code movieId} is assigned by TMDB (it is also the JSON file name), so there is
 * deliberately no {@code @GeneratedValue} — that makes re-importing the same file converge on the
 * same row instead of duplicating it.
 */
@Data
@Entity
@Table(name = "tmdb_movies")
public class TmdbMovie {

  @Id
  @Column(name = "movie_id")
  private Long movieId;

  // Titles observed up to 222 chars on the full corpus, so the 255 default is uncomfortably
  // close: pinned at 512.
  @Column(name = "title", nullable = false, length = 512)
  private String title;

  // width 512: original titles behave like titles and can match them closely.
  @Column(name = "original_title", nullable = false, length = 512)
  private String originalTitle;

  // Observed max 1000 in a 30k sample; 1/3 of the corpus exceeds 255.
  @Column(name = "overview", length = 2048)
  private String overview;

  // width 1024: taglines have plenty of headroom (max seen 249) but run free-form.
  @Column(name = "tagline", length = 1024)
  private String tagline;

  @Column(name = "status")
  private String status;

  @Column(name = "original_language")
  private String originalLanguage;

  @Column(name = "adult", nullable = false)
  private boolean adult;

  @Column(name = "video", nullable = false)
  private boolean video;

  @Column(name = "softcore", nullable = false)
  private boolean softcore;

  @Column(name = "imdb_id")
  private String imdbId;

  @Column(name = "release_date")
  private LocalDate releaseDate;

  @Column(name = "runtime")
  private Integer runtime;

  @Column(name = "budget", nullable = false)
  private Long budget;

  @Column(name = "revenue", nullable = false)
  private Long revenue;

  @Column(name = "popularity", nullable = false)
  private Double popularity;

  @Column(name = "vote_average", nullable = false)
  private Double voteAverage;

  @Column(name = "vote_count", nullable = false)
  private Long voteCount;

  // width 8192: homepage URLs were re-measured at 5,199 chars on the full corpus, overflowing both
  // the 255 default and the original 1024 choice.
  @Column(name = "homepage", length = 8192)
  private String homepage;

  @Column(name = "poster_path")
  private String posterPath;

  @Column(name = "backdrop_path")
  private String backdropPath;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "collection_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TmdbCollection collection;

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "tmdb_movie_genres",
      joinColumns = @JoinColumn(name = "movie_id"),
      inverseJoinColumns = @JoinColumn(name = "genre_id"))
  private Set<TmdbGenre> genres = new LinkedHashSet<>();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "tmdb_movie_keywords",
      joinColumns = @JoinColumn(name = "movie_id"),
      inverseJoinColumns = @JoinColumn(name = "keyword_id"))
  private Set<TmdbKeyword> keywords = new LinkedHashSet<>();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "tmdb_movie_companies",
      joinColumns = @JoinColumn(name = "movie_id"),
      inverseJoinColumns = @JoinColumn(name = "company_id"))
  private Set<TmdbCompany> productionCompanies = new LinkedHashSet<>();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "tmdb_movie_countries",
      joinColumns = @JoinColumn(name = "movie_id"),
      inverseJoinColumns = @JoinColumn(name = "iso_3166_1"))
  private Set<TmdbCountry> productionCountries = new LinkedHashSet<>();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "tmdb_movie_languages",
      joinColumns = @JoinColumn(name = "movie_id"),
      inverseJoinColumns = @JoinColumn(name = "iso_639_1"))
  private Set<TmdbLanguage> spokenLanguages = new LinkedHashSet<>();

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "tmdb_movie_origin_countries",
      joinColumns = @JoinColumn(name = "movie_id"),
      inverseJoinColumns = @JoinColumn(name = "iso_3166_1"))
  private Set<TmdbCountry> originCountries = new LinkedHashSet<>();

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<TmdbMovieCast> cast = new LinkedHashSet<>();

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<TmdbMovieCrew> crew = new LinkedHashSet<>();

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<TmdbReview> reviews = new LinkedHashSet<>();

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<TmdbMovieSimilar> similarMovies = new LinkedHashSet<>();

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<TmdbMovieRecommendation> recommendations = new LinkedHashSet<>();
}
