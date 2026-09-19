package com.codelab.movies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class GetMovieResponse {

    private final Movie movie;
    private final List<MovieCredit> movieCredits;

}
