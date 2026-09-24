package com.codelab.movies.autoconfigure;

import com.codelab.movies.*;
import com.codelab.movies.service.MovieCreditService;
import com.codelab.movies.service.MovieRoleService;
import com.codelab.movies.service.MovieService;
import com.codelab.movies.service.PersonService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
  MovieController.class,
  MovieCreditController.class,
  MovieRoleController.class,
  PersonController.class,
  MovieService.class,
  MovieCreditService.class,
  MovieRoleService.class,
  PersonService.class
})
public class MoviesAutoConfiguration {}
