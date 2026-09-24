package com.codelab.movies.service;

import com.codelab.movies.model.CreateMovieRequest;
import com.codelab.movies.model.MovieDto;
import com.codelab.movies.repository.MovieRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieService {

  private final MovieRepository repository;

  public List<MovieDto> getAll() {
    return repository.findAll().stream().map(MovieDto::from).toList();
  }

  public MovieDto getById(Long id) {
    return MovieDto.from(
        repository.findById(id).orElseThrow(() -> new RuntimeException("Movie not found")));
  }

  public MovieDto create(CreateMovieRequest movie) {
    return MovieDto.from(repository.save(movie.toEntity()));
  }
}
