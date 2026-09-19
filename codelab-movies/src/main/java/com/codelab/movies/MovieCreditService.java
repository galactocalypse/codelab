package com.codelab.movies;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieCreditService {

    private final MovieCreditRepository repository;
    private final MovieRepository movieRepository;
    private final MovieRoleRepository roleRepository;
    private final PersonRepository personRepository;

    public List<MovieCredit> getAllByMovieId(Long id) {
        return repository.findByMovie_Id(id);
    }

    public List<MovieCredit> getAllByPersonId(Long id) {
        return repository.findByPerson_Id(id);
    }

    public MovieCredit create(CreateMovieCreditRequest request) {
        MovieCredit credit = new MovieCredit();

        credit.setMovie(movieRepository.getReferenceById(request.getMovieId()));
        credit.setPerson(personRepository.getReferenceById(request.getPersonId()));
        credit.setRole(roleRepository.getReferenceById(request.getRoleId()));

        return repository.save(credit);
    }

}
