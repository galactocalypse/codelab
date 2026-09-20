package com.codelab.movies.service;

import com.codelab.movies.entity.MovieRole;
import com.codelab.movies.model.CreateMovieRoleRequest;
import com.codelab.movies.model.MovieRoleDto;
import com.codelab.movies.repository.MovieRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieRoleService {

    private final MovieRoleRepository repository;

    public List<MovieRoleDto> getAll() {
        return repository.findAll().stream().map(MovieRoleDto::from).toList();
    }

    public MovieRoleDto getById(Long id) {
        return MovieRoleDto.from(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie role not found")));
    }

    public MovieRoleDto create(CreateMovieRoleRequest role) {
        return MovieRoleDto.from(repository.save(role.toEntity()));
    }

}
