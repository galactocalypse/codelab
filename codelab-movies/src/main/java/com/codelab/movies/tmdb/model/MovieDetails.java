package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDetails {

  private boolean adult;

  @JsonProperty("backdrop_path")
  private String backdropPath;

  @JsonProperty("belongs_to_collection")
  private TmdbCollectionRef belongsToCollection;

  private long budget;
  private List<Genre> genres;
  private String homepage;
  private long id;

  @JsonProperty("imdb_id")
  private String imdbId;

  @JsonProperty("origin_country")
  private List<String> originCountry;

  @JsonProperty("original_language")
  private String originalLanguage;

  @JsonProperty("original_title")
  private String originalTitle;

  private String overview;
  private double popularity;

  @JsonProperty("poster_path")
  private String posterPath;

  @JsonProperty("production_companies")
  private List<ProductionCompany> productionCompanies;

  @JsonProperty("production_countries")
  private List<ProductionCountry> productionCountries;

  @JsonProperty("release_date")
  private String releaseDate;

  private long revenue;
  private int runtime;
  private boolean softcore;

  @JsonProperty("spoken_languages")
  private List<SpokenLanguage> spokenLanguages;

  private String status;
  private String tagline;
  private String title;
  private boolean video;

  @JsonProperty("vote_average")
  private double voteAverage;

  @JsonProperty("vote_count")
  private long voteCount;

  private Credits credits;
  private PagedResult<Review> reviews;
  private PagedResult<MovieSummary> similar;
  private PagedResult<MovieSummary> recommendations;
  private KeywordsResponse keywords;
}
