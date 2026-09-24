package com.codelab.movies;

import com.codelab.movies.model.CreateMovieRoleRequest;
import com.codelab.movies.model.MovieRoleDto;
import com.codelab.movies.service.MovieRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie-roles")
@RequiredArgsConstructor
@Tag(name = "Movie Role")
public class MovieRoleController {

  private final MovieRoleService service;

  @GetMapping
  public List<MovieRoleDto> getAll() {
    return service.getAll();
  }

  @GetMapping("/{id}")
  public MovieRoleDto getById(@PathVariable("id") Long id) {
    return service.getById(id);
  }

  @PostMapping
  public MovieRoleDto create(@RequestBody CreateMovieRoleRequest role) {
    return service.create(role);
  }
}
