package com.codelab.movies;

import com.codelab.movies.entity.MovieCredit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieCreditRepository extends JpaRepository<MovieCredit, Long> {

    @EntityGraph(attributePaths = {"person", "role"})
    List<MovieCredit> findByMovie_Id(Long id);

    @EntityGraph(attributePaths = {"movie", "role"})
    List<MovieCredit> findByPerson_Id(Long id);

}
