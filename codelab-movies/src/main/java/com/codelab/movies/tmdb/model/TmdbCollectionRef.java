package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Shape of TMDB's {@code belongs_to_collection} block. Null for the ~97% of movies without one. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCollectionRef {

  private long id;
  private String name;

  @JsonProperty("poster_path")
  private String posterPath;

  @JsonProperty("backdrop_path")
  private String backdropPath;
}
