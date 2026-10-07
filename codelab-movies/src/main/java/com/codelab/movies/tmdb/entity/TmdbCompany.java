package com.codelab.movies.tmdb.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tmdb_companies")
public class TmdbCompany {

  @Id
  @Column(name = "company_id")
  private Long companyId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "logo_path")
  private String logoPath;

  @Column(name = "origin_country")
  private String originCountry;
}
