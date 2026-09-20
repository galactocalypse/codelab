package com.codelab.movies;

import com.codelab.movies.entity.Movie;
import com.codelab.movies.model.CreateMovieRequest;
import com.codelab.movies.model.GetMovieResponse;
import com.codelab.movies.model.MovieDto;
import com.codelab.movies.service.MovieCreditService;
import com.codelab.movies.service.MovieService;
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
    public List<MovieDto> getAll() {
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
    public MovieDto create(@RequestBody CreateMovieRequest movie) {
        return service.create(movie);
    }

}
