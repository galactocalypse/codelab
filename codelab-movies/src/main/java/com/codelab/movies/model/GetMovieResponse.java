package com.codelab.movies.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class GetMovieResponse {

    private final MovieDto movie;
    private final List<MovieCreditDto> movieCredits;

}
