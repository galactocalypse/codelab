package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tmdb_keywords")
public class TmdbKeyword {

  @Id
  @Column(name = "keyword_id")
  private Long keywordId;

  @Column(name = "name", nullable = false)
  private String name;
}
