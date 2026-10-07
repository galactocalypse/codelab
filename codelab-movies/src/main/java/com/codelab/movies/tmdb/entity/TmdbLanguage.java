package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tmdb_languages")
public class TmdbLanguage {

  @Id
  @Column(name = "iso_639_1", length = 10)
  private String iso6391;

  @Column(name = "name")
  private String name;

  @Column(name = "english_name")
  private String englishName;
}
