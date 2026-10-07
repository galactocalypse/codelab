package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tmdb_genres")
public class TmdbGenre {

  @Id
  @Column(name = "genre_id")
  private Long genreId;

  @Column(name = "name", nullable = false)
  private String name;
}
