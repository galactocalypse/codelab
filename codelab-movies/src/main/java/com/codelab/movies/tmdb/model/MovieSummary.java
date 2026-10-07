package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** Lighter movie shape used inside "similar" / "recommendations" entries. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieSummary {
  private boolean adult;

  @JsonProperty("backdrop_path")
  private String backdropPath;

  private long id;
  private String title;

  @JsonProperty("original_title")
  private String originalTitle;

  private String overview;

  @JsonProperty("poster_path")
  private String posterPath;

  @JsonProperty("media_type")
  private String mediaType;

  @JsonProperty("original_language")
  private String originalLanguage;

  @JsonProperty("genre_ids")
  private List<Long> genreIds;

  private double popularity;

  @JsonProperty("release_date")
  private String releaseDate;

  private boolean softcore;
  private boolean video;

  @JsonProperty("vote_average")
  private double voteAverage;

  @JsonProperty("vote_count")
  private long voteCount;
}
