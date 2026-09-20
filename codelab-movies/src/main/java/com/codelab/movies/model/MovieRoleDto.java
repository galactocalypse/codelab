package com.codelab.movies.model;

import com.codelab.movies.entity.MovieRole;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieRoleDto {

    private Long id;

    private String name;

    public static MovieRoleDto from(MovieRole role) {
        return new MovieRoleDto(role.getId(), role.getName());
    }

}
