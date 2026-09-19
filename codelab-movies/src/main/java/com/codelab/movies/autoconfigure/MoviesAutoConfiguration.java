package com.codelab.movies.autoconfigure;

import com.codelab.movies.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EntityScan(basePackageClasses = {Movie.class, Person.class, MovieCredit.class, MovieRole.class})
@EnableJpaRepositories(basePackageClasses = MovieRepository.class)
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
public class MoviesAutoConfiguration {

}
