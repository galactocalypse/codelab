package com.codelab.movies.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class GetMovieResponse {

  private final MovieDto movie;
  private final List<MovieCreditDto> movieCredits;
}
