package com.codelab.movies.entity;

import com.codelab.movies.model.MovieDto;
import com.codelab.movies.model.MovieRoleDto;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "movie_roles")
public class MovieRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

}
