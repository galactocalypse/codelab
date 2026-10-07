package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpokenLanguage {
  @JsonProperty("english_name")
  private String englishName;

  @JsonProperty("iso_639_1")
  private String iso6391;

  private String name;
}
