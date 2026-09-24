package com.codelab.movies;

import com.codelab.movies.model.CreatePersonRequest;
import com.codelab.movies.model.PersonDto;
import com.codelab.movies.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@Tag(name = "Person")
public class PersonController {

  private final PersonService service;

  @GetMapping
  public List<PersonDto> getAll() {
    return service.getAll();
  }

  @GetMapping("/{id}")
  public PersonDto getById(@PathVariable("id") Long id) {
    return service.getById(id);
  }

  @PostMapping
  public PersonDto create(@RequestBody CreatePersonRequest person) {
    return service.create(person.toEntity());
  }
}
