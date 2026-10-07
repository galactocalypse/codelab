package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductionCompany {
  private long id;

  @JsonProperty("logo_path")
  private String logoPath;

  private String name;

  @JsonProperty("origin_country")
  private String originCountry;
}
