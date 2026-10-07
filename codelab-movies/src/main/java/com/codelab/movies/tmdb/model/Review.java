package com.codelab.movies.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Entry of the "reviews" block; sample results array was empty, so this follows TMDB's documented
 * shape.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Review {
  private String id;
  private String author;

  @JsonProperty("author_details")
  private AuthorDetails authorDetails;

  private String content;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  private String updatedAt;

  private String url;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AuthorDetails {
    private String name;
    private String username;

    @JsonProperty("avatar_path")
    private String avatarPath;

    private Double rating;
  }
}
