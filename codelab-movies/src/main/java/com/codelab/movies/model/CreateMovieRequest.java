package com.codelab.movies.model;

import com.codelab.movies.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateMovieRequest {

    private String name;

    public Movie toEntity() {
        Movie movie = new Movie();
        movie.setName(name);
        return movie;
    }
}
