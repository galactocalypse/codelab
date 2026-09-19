package com.codelab.movies;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movie-credits")
@RequiredArgsConstructor
@Tag(name = "Movie Credit")
public class MovieCreditController {

    private final MovieCreditService service;

    @GetMapping("/by-movie/{movieId}")
    public List<MovieCredit> getMovieCredits(@PathVariable("movieId") Long movieId) {
        return service.getAllByMovieId(movieId);
    }

    @GetMapping("/by-movie/{personId}")
    public List<MovieCredit> getPersonCredits(@PathVariable("personId") Long personId) {
        return service.getAllByPersonId(personId);
    }

    @PostMapping
    public MovieCredit create(@RequestBody CreateMovieCreditRequest request) {
        return service.create(request);
    }

}
