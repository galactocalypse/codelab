package com.codelab.movies.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateMovieCreditRequest {
    private Long movieId;
    private Long personId;
    private Long roleId;

    @JsonCreator
    public CreateMovieCreditRequest(@JsonProperty("movieId") Long movieId, @JsonProperty("personId") Long personId, @JsonProperty("roleId") Long roleId) {
        this.movieId = movieId;
        this.personId = personId;
        this.roleId = roleId;
    }
}
