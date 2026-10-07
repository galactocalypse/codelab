package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastMember {

  private long id;
  private boolean adult;
  private int gender;

  @JsonProperty("known_for_department")
  private String knownForDepartment;

  private String name;

  @JsonProperty("original_name")
  private String originalName;

  private double popularity;

  @JsonProperty("profile_path")
  private String profilePath;

  @JsonProperty("cast_id")
  private long castId;

  private String character;

  @JsonProperty("credit_id")
  private String creditId;

  private int order;
}
