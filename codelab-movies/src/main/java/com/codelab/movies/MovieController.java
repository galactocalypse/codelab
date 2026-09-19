package com.codelab.movies;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@AllArgsConstructor
@Tag(name = "Movie")
public class MovieController {

    private final MovieService service;
    private final MovieCreditService movieCreditService;

    @GetMapping
    public List<Movie> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public GetMovieResponse getById(@PathVariable("id") Long id, @RequestParam(defaultValue = "false") Boolean includeCredits) {
        GetMovieResponse.GetMovieResponseBuilder response = GetMovieResponse.builder();
        response.movie(service.getById(id));
        if (includeCredits) {
            response.movieCredits(movieCreditService.getAllByMovieId(id));
        }
        return response.build();
    }

    @PostMapping
    public Movie create(@RequestBody Movie movie) {
        return service.create(movie);
    }

}
