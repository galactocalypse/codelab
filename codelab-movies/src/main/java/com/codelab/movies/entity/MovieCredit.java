package com.codelab.movies.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
    name = "movie_credits",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_movie_credit_movie_person_role",
          columnNames = {"movie_id", "person_id", "role_id"})
    })
public class MovieCredit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movie_id", nullable = false)
  private Movie movie;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private MovieRole role;
}
