package com.codelab.movies.service;

import com.codelab.movies.entity.Person;
import com.codelab.movies.model.PersonDto;
import com.codelab.movies.repository.PersonRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService {

  private final PersonRepository repository;

  public List<PersonDto> getAll() {
    return repository.findAll().stream().map(PersonDto::from).toList();
  }

  public PersonDto getById(Long id) {
    return PersonDto.from(
        repository.findById(id).orElseThrow(() -> new RuntimeException("Person not found")));
  }

  public PersonDto create(Person Person) {
    return PersonDto.from(repository.save(Person));
  }
}
