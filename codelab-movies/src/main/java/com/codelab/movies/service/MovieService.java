package com.codelab.movies;

import com.codelab.movies.entity.Movie;
import com.codelab.movies.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository repository;

    public List<Movie> getAll() {
        return repository.findAll();
    }

    public Movie getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
    }

    public Movie create(Movie movie) {
        return repository.save(movie);
    }

}
