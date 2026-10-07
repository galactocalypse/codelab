package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * A TMDB person, shared by cast and crew. The same person id routinely appears in both credit lists
 * — and twice within crew when they hold more than one job — with identical attributes.
 */
@Data
@Entity
@Table(name = "tmdb_people")
public class TmdbPerson {

  @Id
  @Column(name = "person_id")
  private Long personId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "original_name")
  private String originalName;

  @Column(name = "gender")
  private Integer gender;

  @Column(name = "known_for_department")
  private String knownForDepartment;

  @Column(name = "popularity")
  private Double popularity;

  @Column(name = "profile_path")
  private String profilePath;

  @Column(name = "adult", nullable = false)
  private boolean adult;
}
