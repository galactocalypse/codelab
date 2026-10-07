package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrewMember {
  private boolean adult;
  private int gender;
  private long id;

  @JsonProperty("known_for_department")
  private String knownForDepartment;

  private String name;

  @JsonProperty("original_name")
  private String originalName;

  private double popularity;

  @JsonProperty("profile_path")
  private String profilePath;

  @JsonProperty("credit_id")
  private String creditId;

  private String department;
  private String job;
}
