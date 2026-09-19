package com.codelab.movies;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movie-roles")
@RequiredArgsConstructor
@Tag(name = "Movie Role")
public class MovieRoleController {

    private final MovieRoleService service;

    @GetMapping
    public List<MovieRole> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MovieRole getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    @PostMapping
    public MovieRole create(@RequestBody MovieRole role) {
        return service.create(role);
    }

}
