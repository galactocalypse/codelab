package com.codelab.movies.model;

import com.codelab.movies.entity.Person;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonDto {

    private long id;

    private String name;

    public static PersonDto from(Person person) {
        return new PersonDto(person.getId(), person.getName());
    }

}
