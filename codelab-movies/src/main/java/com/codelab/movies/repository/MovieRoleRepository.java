package com.codelab.movies;

import com.codelab.movies.entity.MovieRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRoleRepository extends JpaRepository<MovieRole, Long> {

}
