package com.codelab.movies.service;

import com.codelab.movies.entity.MovieCredit;
import com.codelab.movies.model.CreateMovieCreditRequest;
import com.codelab.movies.model.MovieCreditDto;
import com.codelab.movies.repository.MovieCreditRepository;
import com.codelab.movies.repository.MovieRepository;
import com.codelab.movies.repository.MovieRoleRepository;
import com.codelab.movies.repository.PersonRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieCreditService {

  private final MovieCreditRepository repository;
  private final MovieRepository movieRepository;
  private final MovieRoleRepository roleRepository;
  private final PersonRepository personRepository;

  public List<MovieCreditDto> getAllByMovieId(Long id) {
    return repository.findByMovie_Id(id).stream()
        .map(MovieCreditDto::fromGetByMovieIdEntity)
        .toList();
  }

  public List<MovieCreditDto> getAllByPersonId(Long id) {
    return repository.findByPerson_Id(id).stream()
        .map(MovieCreditDto::fromGetByPersonIdEntity)
        .toList();
  }

  public MovieCreditDto create(CreateMovieCreditRequest request) {
    MovieCredit credit = new MovieCredit();

    credit.setMovie(movieRepository.getReferenceById(request.getMovieId()));
    credit.setPerson(personRepository.getReferenceById(request.getPersonId()));
    credit.setRole(roleRepository.getReferenceById(request.getRoleId()));

    return MovieCreditDto.fromCreatedEntity(repository.save(credit));
  }
}
