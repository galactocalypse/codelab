package com.codelab.movies.model;

import com.codelab.movies.entity.MovieRole;

public class CreateMovieRoleRequest {

  private String name;

  public MovieRole toEntity() {
    MovieRole movieRole = new MovieRole();
    movieRole.setName(name);
    return movieRole;
  }
}
