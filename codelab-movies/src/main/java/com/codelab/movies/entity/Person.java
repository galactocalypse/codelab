package com.codelab.movies.entity;

import com.codelab.movies.model.MovieDto;
import com.codelab.movies.model.PersonDto;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

}
