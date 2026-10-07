package com.codelab.movies;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Test-only entry point. The module is a library, so it has no application class of its own; the
 * slice tests need a {@code @SpringBootConfiguration} above {@link com.codelab.movies.tmdb} to
 * locate entities and repositories from.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class MoviesTestApplication {}
