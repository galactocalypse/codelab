package com.codelab.movies.model;

import com.codelab.movies.entity.MovieCredit;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieCreditDto {

  private Long id;
  private MovieDto movie;
  private PersonDto person;
  private MovieRoleDto role;

  public static MovieCreditDto fromCreatedEntity(MovieCredit credit) {
    return new MovieCreditDto(credit.getId(), null, null, null);
  }

  public static MovieCreditDto fromGetByMovieIdEntity(MovieCredit credit) {
    return new MovieCreditDto(
        credit.getId(),
        null,
        PersonDto.from(credit.getPerson()),
        MovieRoleDto.from(credit.getRole()));
  }

  public static MovieCreditDto fromGetByPersonIdEntity(MovieCredit credit) {
    return new MovieCreditDto(
        credit.getId(),
        MovieDto.from(credit.getMovie()),
        null,
        MovieRoleDto.from(credit.getRole()));
  }
}
