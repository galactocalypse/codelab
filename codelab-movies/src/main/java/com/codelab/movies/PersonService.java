package com.codelab.movies;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    
    public List<Person> getAll() {
        return repository.findAll();
    }

    public Person getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
    }

    public Person create(Person Person) {
        return repository.save(Person);
    }
}
