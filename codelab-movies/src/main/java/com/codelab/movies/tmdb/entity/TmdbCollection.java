package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tmdb_collections")
public class TmdbCollection {

  @Id
  @Column(name = "collection_id")
  private Long collectionId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "poster_path")
  private String posterPath;

  @Column(name = "backdrop_path")
  private String backdropPath;
}
