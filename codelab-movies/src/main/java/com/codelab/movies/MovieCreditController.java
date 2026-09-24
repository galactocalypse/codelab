package com.codelab.movies;

import com.codelab.movies.model.CreateMovieCreditRequest;
import com.codelab.movies.model.MovieCreditDto;
import com.codelab.movies.service.MovieCreditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie-credits")
@RequiredArgsConstructor
@Tag(name = "Movie Credit")
public class MovieCreditController {

  private final MovieCreditService service;

  @GetMapping("/by-movie/{movieId}")
  public List<MovieCreditDto> getMovieCredits(@PathVariable("movieId") Long movieId) {
    return service.getAllByMovieId(movieId);
  }

  @GetMapping("/by-movie/{personId}")
  public List<MovieCreditDto> getPersonCredits(@PathVariable("personId") Long personId) {
    return service.getAllByPersonId(personId);
  }

  @PostMapping
  public MovieCreditDto create(@RequestBody CreateMovieCreditRequest request) {
    return service.create(request);
  }
}
