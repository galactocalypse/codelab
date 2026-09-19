package com.codelab.movies;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieRoleService {

    private final MovieRoleRepository repository;

    public List<MovieRole> getAll() {
        return repository.findAll();
    }

    public MovieRole getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie role not found"));
    }

    public MovieRole create(MovieRole role) {
        return repository.save(role);
    }

}
