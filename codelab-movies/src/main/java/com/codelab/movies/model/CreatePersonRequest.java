package com.codelab.movies.model;

import com.codelab.movies.entity.Movie;
import com.codelab.movies.entity.Person;

public class CreatePersonRequest {

    private String name;

    public Person toEntity() {
        Person person = new Person();
        person.setName(name);
        return person;
    }

}
