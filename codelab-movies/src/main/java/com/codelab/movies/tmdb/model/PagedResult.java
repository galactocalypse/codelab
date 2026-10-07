package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** Shared shape of the "reviews", "similar" and "recommendations" blocks. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResult<T> {
  private int page;
  private List<T> results;

  @JsonProperty("total_pages")
  private int totalPages;

  @JsonProperty("total_results")
  private int totalResults;
}
