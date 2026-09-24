package com.codelab.movies.model;

import com.codelab.movies.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieDto {

  private long id;
  private String name;

  public static MovieDto from(Movie movie) {
    return new MovieDto(movie.getId(), movie.getName());
  }
}
